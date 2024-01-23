from pathlib import Path
import re
from textwrap import dedent, indent
from itertools import chain
import subprocess

root = Path("~/work/appsmith-ce").expanduser()
server_root = root / "app/server"
FILE_CONTENTS_CACHE = {}

MONO_WRAPPER = (
    "Mono.fromSupplier(() -> %s.orElse(null)).subscribeOn(Schedulers.boundedElastic())"
)
MONO_WRAPPER_NON_OPTIONAL = (
    "Mono.fromSupplier(() -> %s).subscribeOn(Schedulers.boundedElastic())"
)
FLUX_WRAPPER = "Mono.fromSupplier(() -> %s).flatMapIterable(o -> o).subscribeOn(Schedulers.boundedElastic())"


def apply(p, tx):
    update_file(p, tx(read_file(p)))


def add_postgres_dep(content: str):
    if "<artifactId>postgresql</artifactId>" in content:
        return content
    return content.replace(
        "<dependencies>",
        """<dependencies>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
        </dependency>""",
        1,
    )


def del_postgres_dep(content: str):
    return content.replace(
        """
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
        </dependency>""",
        "",
    )


def repo_interfaces(domain):
    return list(
        filter(
            Path.exists,
            (
                (
                    server_root
                    / f"appsmith-server/src/main/java/com/appsmith/server/repositories/{'ce/' if 'CE' in g else ''}{g}.java"
                )
                for g in [
                    f"Custom{domain}RepositoryCE",
                    f"{domain}RepositoryCE",
                    f"Custom{domain}Repository",
                    f"{domain}Repository",
                ]
            ),
        )
    )


def repo_classes(domain):
    return list(
        filter(
            Path.exists,
            (
                (
                    server_root
                    / f"appsmith-server/src/main/java/com/appsmith/server/repositories/{'ce/' if 'CE' in g else ''}{g}.java"
                )
                for g in [
                    f"Custom{domain}RepositoryCEImpl",
                    # f"{domain}RepositoryCE",
                    # f"Custom{domain}Repository",
                    # f"{domain}Repository",
                ]
            ),
        )
    )


def switch_repo_types(domain):
    for full_path in chain(repo_interfaces(domain), repo_classes(domain)):
        content = (
            read_file(full_path)
            .replace("    Mono<", "    Optional<")
            .replace("    Flux<", "    List<")
            .replace(" public Mono<", " public Optional<")
            .replace(" public Flux<", " public List<")
        )
        if "import java.util.List;" not in content:
            content = content.replace(
                ";\n\npublic interface", ";\nimport java.util.List;\n\npublic interface"
            )
        if "import java.util.Optional;" not in content:
            content = content.replace(
                ";\n\npublic interface",
                ";\nimport java.util.Optional;\n\npublic interface",
            )
        update_file(full_path, content)


def generate_base_cake():
    base_cake_cls = f"""\
        package com.appsmith.server.repositories.cakes;

        import com.appsmith.external.models.BaseDomain;
        import lombok.RequiredArgsConstructor;
        import org.springframework.data.repository.CrudRepository;
        import reactor.core.publisher.Mono;
        import reactor.core.scheduler.Schedulers;

        import java.util.Optional;

        @RequiredArgsConstructor
        public abstract class BaseCake<T extends BaseDomain> {{
            private final CrudRepository<T, String> repository;

            public Mono<T> save(T entity) {{
                return {MONO_WRAPPER % "Optional.of(repository.save(entity))"};
            }}

            public Mono<T> findById(String id) {{
                return {MONO_WRAPPER % "repository.findById(id)"};
            }}

        }}
    """

    update_file(
        server_root
        / "appsmith-server/src/main/java/com/appsmith/server/repositories/cakes/BaseCake.java",
        dedent(base_cake_cls),
    )


def generate_cake_class(domain):
    methods = set()
    reactor_methods = []

    extra_repo_interfaces = ["BaseRepository"]
    if any(
        "extends AppsmithRepository<" in read_file(p) for p in repo_interfaces(domain)
    ):
        extra_repo_interfaces.append("AppsmithRepository")

    for full_path in chain(
        repo_interfaces(domain),
        (
            (
                server_root
                / f"appsmith-server/src/main/java/com/appsmith/server/repositories/{f}.java"
            )
            for f in extra_repo_interfaces
        ),
    ):
        content = read_file(full_path)
        if domain not in full_path.name:
            content = re.sub(r"\bT\b", domain, content)
            content = re.sub(r"\bID\b", "String", content)
        # because of unambiguous error for this class with the one in `domains` package.
        content = re.sub(r"\bCollection<", "java.util.Collection<", content)
        methods.update(
            re.sub(r"\s+", " ", m.strip().replace("default ", "")).replace("( ", "(")
            for m in re.findall(
                r"^ {4}(?:default )?[\w<>?]+\s+\w+\([^)]*\)",
                content,
                re.DOTALL | re.MULTILINE,
            )
        )

    for method in sorted(methods):
        ret_type, signature = method.split(None, 1)

        if ret_type.startswith("Optional"):
            ret_type = ret_type.replace("Optional", "Mono")
            wrapper = MONO_WRAPPER
        elif ret_type.startswith(("List", "Iterable")):
            ret_type = ret_type.replace("List", "Flux").replace("Iterable", "Flux")
            wrapper = FLUX_WRAPPER
        elif not ret_type.islower():
            ret_type = "Mono<" + ret_type + ">"
            wrapper = MONO_WRAPPER_NON_OPTIONAL
        else:
            wrapper = "%s"

        call = re.sub(
            r"[A-Za-z.]+?(<[^<>]+?>|<[^\s]+?>)?\s(\w+)([,)])", r"\2\3", signature
        )
        reactor_methods.append(
            "public "
            + ret_type
            + " "
            + signature
            + " {\n    return "
            + (wrapper % ("repository." + call))
            + ";\n}\n"
        )

    content = indent("\n".join(reactor_methods), " " * 4)

    imports = set()
    if "<UpdateResult>" in content:
        imports.add("com.mongodb.client.result.UpdateResult")
    if "PluginTypeAndCountDTO" in content:
        imports.add("com.appsmith.server.dtos.PluginTypeAndCountDTO")
    if "CustomJSLibContextDTO" in content:
        imports.add("com.appsmith.server.dtos.CustomJSLibContextDTO")

    imports_code = "".join(f"import {i};" for i in imports)

    prefix = dedent(
        f"""\
    package com.appsmith.server.repositories.cakes;

    import com.appsmith.external.models.*;
    import com.appsmith.server.acl.AclPermission;
    import com.appsmith.server.domains.*;
    import com.appsmith.server.projections.*;
    import com.appsmith.server.repositories.*;
    import org.springframework.stereotype.Component;
    import org.springframework.data.domain.Sort;
    import reactor.core.publisher.Flux;
    import reactor.core.publisher.Mono;
    import org.springframework.data.mongodb.core.query.*;
    import com.mongodb.bulk.BulkWriteResult;
    import com.mongodb.client.result.InsertManyResult;
    import com.querydsl.core.types.dsl.StringPath;
    import reactor.core.scheduler.Schedulers;
    {imports_code}

    import java.util.List;
    import java.util.Map;
    import java.util.Optional;
    import java.util.Set;

    @Component
    public class {domain}RepositoryCake extends BaseCake<{domain}> {{
        private final {domain}Repository repository;

        public {domain}RepositoryCake({domain}Repository repository) {{
            super(repository);
            this.repository = repository;
        }}

        // From CrudRepository
        public Flux<{domain}> saveAll(Iterable<{domain}> entities) {{
            return {FLUX_WRAPPER % "repository.saveAll(entities)"};
        }}
        // End from CrudRepository
    """
    )

    gen_src = prefix.strip() + "\n\n" + content + "\n}\n"

    try:
        repo_class_path = next(server_root.rglob(domain + "Repository.java"))
    except StopIteration:
        print(f"Could not find {domain}Repository.java")
        return

    update_file(
        repo_class_path.parent / f"cakes/{domain}RepositoryCake.java",
        gen_src,
    )


def use_cake(domain):
    for path in chain(
        server_root.rglob("*ServiceImpl.java"),
        server_root.rglob("*ServiceCEImpl.java"),
        server_root.rglob("*ServiceCECompatibleImpl.java"),
        server_root.rglob("AuthenticationSuccessHandler*.java"),
        server_root.rglob("PingScheduledTask*Impl.java"),
        server_root.rglob("UserChangedHandler*Impl.java"),
        server_root.glob("**/solutions/*.java"),
        server_root.glob("**/solutions/ce/*.java"),
        server_root.glob("**/com/appsmith/server/helpers/*.java"),
        server_root.glob("**/com/appsmith/server/helpers/ce/*.java"),
    ):
        out = re.sub(rf"\b({domain}Repository)\b", r"\1Cake", read_file(path))
        out = re.sub(
            r"(import com\.appsmith\.server\.repositories\.)(\w+?Cake;)",
            r"\1cakes.\2",
            out,
        )
        update_file(path, out)


def to_entity(domain: str):
    path = next(server_root.rglob(f"{domain}.java"))

    content = read_file(path)

    content = content.replace("@Document", "@Entity").replace(
        "org.springframework.data.mongodb.core.mapping.Document",
        "jakarta.persistence.Entity",
    )

    if " extends BaseDomain {" not in content and "@Id" not in content:
        id_field = indent(
            "\n".join(
                [
                    # todo: need import statements for these.
                    "@Id",
                    "@GeneratedValue(strategy = GenerationType.UUID)",
                    "private String id;",
                ]
            ),
            " " * 4,
        )
        content = re.sub(
            f"(public class {domain} (implements AppsmithDomain )?{{)\n+",
            f"\\1\n\n{id_field}\n\n",
            content,
        )

    update_file(path, content)


def read_file(path: Path):
    if path not in FILE_CONTENTS_CACHE:
        FILE_CONTENTS_CACHE[path] = path.read_text()
    return FILE_CONTENTS_CACHE[path]


def update_file(path: Path, content: str):
    if not path.exists() or path.read_text() != content:
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(content)
        FILE_CONTENTS_CACHE[path] = content


def convert(domain):
    print("Bake", domain)
    domain_class_content = read_file(next(root.glob(f"app/server/**/{domain}.java")))
    if (
        "@Entity\n" not in domain_class_content
        and "@Document\n" not in domain_class_content
    ):
        raise ValueError(f"{domain} is not a DB Document")

    to_entity(domain)
    switch_repo_types(domain)
    generate_cake_class(domain)
    use_cake(domain)


def main():
    apply(root / "app/server/appsmith-interfaces/pom.xml", add_postgres_dep)
    apply(root / "app/server/appsmith-plugins/postgresPlugin/pom.xml", del_postgres_dep)

    generate_base_cake()

    convert("ActionCollection")
    convert("Application")
    convert("UserData")
    convert("User")
    convert("Workspace")
    convert("DatasourceStorageStructure")
    convert("NewPage")
    convert("Plugin")
    convert("NewAction")
    convert("PermissionGroup")
    convert("ApplicationSnapshot")
    convert("CustomJSLib")
    convert("GitDeployKeys")
    convert("Datasource")
    convert("Theme")
    convert("EmailVerificationToken")
    convert("Config")
    convert("PasswordResetToken")
    convert("DatasourceStorage")
    convert("Tenant")
    convert("Collection")
    convert("Asset")

    print("Format cakes")
    subprocess.check_call(
        [
            "mvn",
            "-Dorg.slf4j.simpleLogger.defaultLogLevel=warn",
            "spotless:apply",
            # r"-DspotlessFiles=appsmith-server/src/main/java/com/appsmith/server/repositories/cakes/.*\.java",
        ],
        cwd=server_root,
    )


if __name__ == "__main__":
    main()
