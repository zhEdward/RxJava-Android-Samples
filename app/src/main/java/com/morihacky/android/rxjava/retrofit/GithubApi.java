package com.morihacky.android.rxjava.retrofit;

import java.util.List;

import retrofit2.http.GET;
import retrofit2.http.Path;
import rx.Observable;

/***
 * retrofit 定义的api （该网站必须是支持 API RESTful style）
 */
public interface GithubApi {

    //http 前缀 就是 https://api.github.com


    /**
     * See https://developer.github.com/v3/repos/#list-contributors
     */
    @GET("/repos/{owner}/{repo}/contributors")
    Observable<List<Contributor>> contributors(@Path("owner") String owner,
                                               @Path("repo") String repo);

    //    @GET("/repos/{owner}/{repo}/contributors")
    List<Contributor> getContributors(@Path("owner") String owner, @Path("repo") String repo);

    /**
     * See https://developer.github.com/v3/users/
     */
    @GET("/users/{user}")
    Observable<User> user(@Path("user") String user);

    /**
     * See https://developer.github.com/v3/users/
     */
    @GET("/users/{user}")
    User getUser(@Path("user") String user);
}