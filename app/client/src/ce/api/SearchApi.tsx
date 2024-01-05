import Api from "api/Api";
import type { ApiResponse } from "api/ApiResponses";
import type { FetchWorkspacesResponse } from "@appsmith/api/WorkspaceApi";
import type { FetchApplicationsResponse } from "@appsmith/api/ApplicationApi";
import type { AxiosPromise } from "axios";

export interface SearchEntitiesRequest {
  entities?: string[];
  keyword: string;
  page?: number;
  limit?: number;
}

export interface SearchEntitiesResponse {
  entities: [];
  keyword: string;
  page: number;
  limit: number;
}

export interface SearchApiResponse extends ApiResponse {
  data: {
    applications: FetchApplicationsResponse[];
    workspaces: FetchWorkspacesResponse[];
  };
}

export class SearchApi extends Api {
  static searchURL = "v1/search-entities";

  static async searchAllEntities(params: {
    keyword: string;
    limit?: number;
  }): Promise<AxiosPromise<SearchApiResponse>> {
    const { keyword, limit = 10 } = params;
    return Api.get(`${SearchApi.searchURL}?keyword=${keyword}&size=${limit}`);
  }
}

export default SearchApi;
