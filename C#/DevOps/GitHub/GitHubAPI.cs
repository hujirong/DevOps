using System;
using RestSharp;
using System.Collections.Generic;


namespace DevOps
{
    /// <summary>
    /// https://github.com/restsharp/RestSharp/wiki/Recommended-Usage
    /// </summary>
    class GitHubAPI
    {
        RestClient client;
        RestSharpAPI restSharpAPI;

        static string baseUrl = "http://github.com/api/v3";

        static string gitHubUser = "svc_appbuilder_prod";
        static string gitHubToken = "028ce4aae8d952c3e65a7632d494316f07e9408e";

        static string myUser = "huj";
        static string myToken = "7e74b0179eda70a8b7a667a6da9f28f7e446ef1f";

        /// <summary>
        /// Initializes a new instance of the <see cref="GitHubAPI"/> class.
        /// </summary>
        /// <param name="client">The RestClient.</param>
        public GitHubAPI(RestSharpAPI restSharpAPI, RestClient client)
        {
            this.restSharpAPI = restSharpAPI;
            this.client = client;
        }

        /// <summary>
        /// Gets a single user, using userid/password for auth
        /// </summary>
        /// <returns>GitHubUser</returns>
        public GitHubUser GetGitHubUser()
        {
            RestRequest request = new RestRequest();
            request.Resource = "/users/huj";
            request.RootElement = "GitHubUser";

            return restSharpAPI.Execute<GitHubUser>(this.client, request);
        }

        /// <summary>
        /// Gets a single user. Using user/token
        /// </summary>
        /// <returns>GitHubUser</returns>
        public GitHubUser GetGitHubUser2()
        {
            RestRequest request = new RestRequest();
            request.Resource = "/users/huj";
            request.RootElement = "GitHubUser";

            return restSharpAPI.Execute<GitHubUser>(this.client, request);
        }


        /// <summary>
        /// http://stackoverflow.com/questions/30133937/how-to-use-oauth2-in-restsharp
        /// </summary>
        /// <returns>GitHubUser</returns>
        public GitHubUser GetGitHubUser3()
        {
            //RestRequest request = new RestRequest(Method.POST);  //empty data
            RestRequest request = new RestRequest();
            request.AddHeader("Content-Type", "application/x-www-form-urlencoded");
            request.AddHeader("Accept", "application/json");
            request.AddParameter("grant_type", "client_credentials");

            request.Resource = "/users/huj";
            request.RootElement = "GitHubUser";

            return restSharpAPI.Execute<GitHubUser>(this.client, request);
        }


        /// <summary>
        /// Gets all users.
        /// </summary>
        /// <returns>List<GitHubUser></returns>
        public List<GitHubUser> GetAllUsers()
        {
            RestRequest request = new RestRequest();
            request.Resource = "/users";
            request.RootElement = "GitHubUser";

            return restSharpAPI.Execute<List<GitHubUser>>(this.client, request);
        }

        public List<GitHubRepo> GetOrgRepos(string org)
        {
            RestRequest request = new RestRequest();
            request.Resource = "/orgs/" + org + "/repos";
            request.RootElement = "GitHubRepo";

            return restSharpAPI.Execute<List<GitHubRepo>>(this.client, request);
        }

        public List<GitHubRepo> GetUserRepos(string user)
        {
            RestRequest request = new RestRequest();
            request.Resource = "/user/repos";
            request.RootElement = "GitHubRepo";

            return restSharpAPI.Execute<List<GitHubRepo>>(this.client, request);
        }

        public List<GitHubTeam> GetOrgTeams(string org)
        {
            RestRequest request = new RestRequest();
            request.Resource = "/orgs/" + org + "/teams";
            request.RootElement = "GitHubTeam";

            return restSharpAPI.Execute<List<GitHubTeam>>(this.client, request);
        }

        public List<GitHubUser> GetTeamMembers(string teamId)
        {
            RestRequest request = new RestRequest();
            request.Resource = "/teams/" + teamId + "/members";
            request.RootElement = "GitHubUser";

            return restSharpAPI.Execute<List<GitHubUser>>(this.client, request);
        }

        static void Main(string[] args)
        {
            try
            {                
                RestSharpAPI restSharpAPI = new RestSharpAPI(baseUrl);
                RestClient client = restSharpAPI.getClient2(myUser, myToken);

                GitHubAPI gitHubAPI = new GitHubAPI(restSharpAPI, client);

                List<GitHubRepo> repos = new List<GitHubRepo>();
                List<GitHubTeam> teams = new List<GitHubTeam>();
                List<GitHubUser> users = new List<GitHubUser>();

                string org = "DevOps";
                //repos = gitHubAPI.GetOrgRepos(org);

                string userId = "huj";
                //repos = gitHubAPI.GetUserRepos(userId);

                teams = gitHubAPI.GetOrgTeams(org);

                foreach (GitHubTeam team in teams) // Loop through List with foreach.
                {
                    Console.WriteLine("Team info: {0}", team.ToString());
                    users = gitHubAPI.GetTeamMembers(team.id);
                    foreach (GitHubUser user in users) // Loop through List with foreach.
                    {
                        Console.WriteLine("User info: {0}", user.ToString());
                    }

                }

                //foreach (GitHubRepo repo in repos) // Loop through List with foreach.
                //{
                //     Console.WriteLine("Repo info: {0}", repo.ToString());
                //}  


                /// User Operations
                //GitHubUser gitHubUser = gitHubAPI.GetGitHubUser();
                //Console.WriteLine("User1 info: {0}", gitHubUser.ToString());

                //GitHubUser gitHubUser2 = gitHubAPI.GetGitHubUser2();
                //Console.WriteLine("User2 info: {0}", gitHubUser2.ToString());

                //GitHubUser gitHubUser3 = gitHubAPI.GetGitHubUser3();
                //Console.WriteLine("User3 info: {0}", gitHubUser3.ToString());

                //List<GitHubUser> users = new List<GitHubUser>();
                //users = restSharpAPI.GetAllUsers();            
                //foreach (GitHubUser user in users) // Loop through List with foreach.
                //{
                //     Console.WriteLine("User info: {0}", user.ldap_dn);
                //}            

                Console.ReadKey(true);
            }
            catch (Exception ex)
            {
                Console.WriteLine("Exception caught: {0}", ex);
                Console.ReadKey(true);
            }
        }
    }

}