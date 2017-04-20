export class AngularJSRestApplicationClient extends RestApplicationClient<ng.IRequestConfig> {
    constructor(baseUrl: string, $http: ng.IHttpService, $q: ng.IQService) {
        super(new AngularJSHttpClient(baseUrl, $http, $q));
    }
}

export class AngularJSRestApplicationClientProvider {

    private baseUrl: string;

    setBaseUrl(baseUrl: string) {
        this.baseUrl = baseUrl;
    }

    $get = ['$http', '$q', ($http: ng.IHttpService, $q: ng.IQService) => {
        return new AngularJSHttpClient(this.baseUrl, $http, $q);
    }];

}
