package com.appsmith.external.models;

import com.appsmith.external.annotations.encryption.Encrypted;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Type;

@Getter
@Setter
@ToString
@NoArgsConstructor
@Entity
public class PEMCertificate implements AppsmithDomain {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    UploadedFile file;

    @Encrypted @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    String password;

    public PEMCertificate(UploadedFile file, String password) {
        this.file = file;
        this.password = password;
    }
}
