package com.morihacky.android.rxjava.retrofit;

/**
 * 使用 gson进行 json-> object 定义的变量 还要返回json终端额一致才可以取得 value
 */
public class Contributor {
    public String login;
    public long contributions;


    /**{
     "login": "JakeWharton",
     "id": 66577,
     "avatar_url": "https://avatars.githubusercontent.com/u/66577?v=3",
     "gravatar_id": "",
     "url": "https://api.github.com/users/JakeWharton",
     "html_url": "https://github.com/JakeWharton",
     "followers_url": "https://api.github.com/users/JakeWharton/followers",
     "following_url": "https://api.github.com/users/JakeWharton/following{/other_user}",
     "gists_url": "https://api.github.com/users/JakeWharton/gists{/gist_id}",
     "starred_url": "https://api.github.com/users/JakeWharton/starred{/owner}{/repo}",
     "subscriptions_url": "https://api.github.com/users/JakeWharton/subscriptions",
     "organizations_url": "https://api.github.com/users/JakeWharton/orgs",
     "repos_url": "https://api.github.com/users/JakeWharton/repos",
     "events_url": "https://api.github.com/users/JakeWharton/events{/privacy}",
     "received_events_url": "https://api.github.com/users/JakeWharton/received_events",
     "type": "User",
     "site_admin": false,
     "contributions": 814
     },
     *
     */

}
