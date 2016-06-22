using System;
using System.IO;
using RestSharp;
using System.Collections.Generic;
using DevOps;

/// <summary>
/// https://github.com/restsharp/RestSharp/wiki/Recommended-Usage
/// </summary>
class NexusAPI
{
    RestClient client;
    RestSharpAPI restSharpAPI;
    static string baseUrl = "http://localhost:8060/nexus/service/local";
    static string user = "admin";
    static string password = "admin123";
        
    /// <summary>
    /// Initializes a new instance of the <see cref="GitHubAPI"/> class.
    /// </summary>
    /// <param name="client">The RestClient.</param>
    public NexusAPI(RestSharpAPI restSharpAPI, RestClient client) {
        this.restSharpAPI = restSharpAPI;
        this.client = client;
    }


    /// <summary>
    /// Gets all users.
    /// </summary>
    /// <returns>List<GitHubUser></returns>
    public List<NexusRepo> GetAllRepos()    
    {
        RestRequest request = new RestRequest();
        //RestRequest request = new RestRequest("/repositories", Method.GET);
        request.Resource = "/repositories";
        //request.RootElement = "NexusRepo";  //Error: Object reference not set to an instance of an object

        return this.restSharpAPI.Execute<List<NexusRepo>>(this.client, request);
        
    }

    public void uploadArtifact()
    {
        RestRequest request = new RestRequest("artifact/maven/content", Method.POST);
        request.RequestFormat = RestSharp.DataFormat.Json;
        request.AddHeader("Content-Type", "x-www-form-urlencoded");
        request.AddParameter("r", "releases");
        request.AddParameter("hasPom", "false");
        request.AddParameter("p", "zip");
        request.AddParameter("e", "zip");
        request.AddParameter("g", "otpp.devops");        
        request.AddParameter("a", "githubSuspsendInactiveADAccounts");
        request.AddParameter("v", "1.0.1");
        request.AddParameter("file", "@C:\\Temp\\Nexus\\githubSuspsendInactiveADAccounts-1.0.1.zip");
        request.AddFile("githubSuspsendInactiveADAccounts-1.0.1.zip", @"C:\\Temp\\Nexus\\githubSuspsendInactiveADAccounts-1.0.1.zip"); //Completed but not uploaded
        
                    
        IRestResponse response = this.restSharpAPI.Execute(this.client, request);
        Console.WriteLine("Response received: {0}", response.ResponseStatus);        

    }

    static void Main(string[] args)
    {
        try
        {
            RestSharpAPI restSharpAPI = new RestSharpAPI(baseUrl);
            //RestClient client = restSharpAPI.getClient();  //works
            RestClient client = restSharpAPI.getClient(user, password); // works

            if (restSharpAPI == null || client == null)
            {
                Console.WriteLine("restSharpAPI or client is null");
                Console.ReadKey(true);
                Environment.Exit(1);
            }

            NexusAPI nexusAPI = new NexusAPI(restSharpAPI, client);
            List<NexusRepo> repos = new List<NexusRepo>();
            repos = nexusAPI.GetAllRepos();
            if (repos == null)
            {
                Console.WriteLine("repos is null");
                Console.ReadKey(true);
                Environment.Exit(1);
            }
            else {
                Console.WriteLine("Size of repos: {0}", repos.Count);
            }

            foreach (NexusRepo repo in repos)
            {             
                Console.WriteLine("Repo info: {0}", repo.ToString());
            }

            nexusAPI.uploadArtifact();

            Console.ReadKey(true);
        }
        catch (Exception ex)
        {
            Console.WriteLine("Exception caught: {0}", ex);
            Console.ReadKey(true);
        }
    }
}

