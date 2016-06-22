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

    /// <summary>
    /// Works
    /// </summary>
    public void uploadArtifact() 
    {
        RestRequest request = new RestRequest("artifact/maven/content", Method.POST);
        // Below two lines are optional
        //request.RequestFormat = RestSharp.DataFormat.Json;
        //request.AddHeader("Content-Type", "x-www-form-urlencoded");
        request.AddParameter("r", "releases");
        request.AddParameter("hasPom", "false");
        request.AddParameter("p", "zip");
        request.AddParameter("e", "zip");
        request.AddParameter("g", "otpp.devops");        
        request.AddParameter("a", "githubSuspsendInactiveADAccounts");
        request.AddParameter("v", "1.0.1");
        request.AddParameter("file", "githubSuspsendInactiveADAccounts-1.0.1.zip");
        request.AddFile("githubSuspsendInactiveADAccounts-1.0.1.zip", "githubSuspsendInactiveADAccounts-1.0.1.zip");
        
        IRestResponse response = this.restSharpAPI.Execute(this.client, request);
        Console.WriteLine("Response received: {0}", response.ResponseStatus);        

    }

    /// <summary>
    /// Additional path to the file
    /// </summary>
    public void uploadArtifact1()
    {
        RestRequest request = new RestRequest("artifact/maven/content", Method.POST);
        // Below two lines are optional
        //request.RequestFormat = RestSharp.DataFormat.Json;
        //request.AddHeader("Content-Type", "x-www-form-urlencoded");
        request.AddParameter("r", "releases");
        request.AddParameter("hasPom", "false");
        request.AddParameter("p", "zip");
        request.AddParameter("e", "zip");
        request.AddParameter("g", "otpp.devops");
        request.AddParameter("a", "githubSuspsendInactiveADAccounts");
        request.AddParameter("v", "1.0.1");
        request.AddParameter("file", "C:/Temp/Nexus/githubSuspsendInactiveADAccounts-1.0.1.zip");
        request.AddFile("githubSuspsendInactiveADAccounts-1.0.1.zip", "C:/Temp/Nexus/githubSuspsendInactiveADAccounts-1.0.1.zip");
        
        IRestResponse response = this.restSharpAPI.Execute(this.client, request);
        Console.WriteLine("Response received: {0}", response.ResponseStatus);

    }

    /// <summary>
    /// https://blog.tyrsius.com/restsharp-file-upload/
    /// </summary>
    public void uploadArtifact2() // can't close stream until all bytes are written
    {
        RestRequest request = new RestRequest("artifact/maven/content/", Method.POST);
        request.RequestFormat = RestSharp.DataFormat.Json;
        request.AlwaysMultipartFormData = true;
        request.AddHeader("Content-Type", "x-www-form-urlencoded");
        request.AddParameter("r", "releases");
        request.AddParameter("hasPom", "false");
        request.AddParameter("p", "zip");
        request.AddParameter("e", "zip");
        request.AddParameter("g", "otpp.devops");
        request.AddParameter("a", "githubSuspsendInactiveADAccounts");
        request.AddParameter("v", "1.0.1");
        request.AddParameter("file", "githubSuspsendInactiveADAccounts-1.0.1.zip");    
        
        string filename = "githubSuspsendInactiveADAccounts-1.0.1.zip";
        FileStream fs = new FileStream(filename, FileMode.Open, FileAccess.Read);
        request.AddFile("file", fs.CopyTo, filename);
        fs.Close();

        IRestResponse response = this.restSharpAPI.Execute(this.client, request);
        Console.WriteLine("Response received: {0}", response.ResponseStatus);
        
    }

    /// <summary>
    /// http://drupal.stackexchange.com/questions/101872/attach-file-to-post-using-c-restsharp-and-drupal-services/143727#143727?newreg=0d4ecba4b7234432831af804a232fdb9
    /// </summary>
    public void uploadArtifact3()  //Caused by: java.lang.IllegalStateException: Form too large 832324>200000
    {
        RestRequest request = new RestRequest("artifact/maven/content/", Method.POST);
        request.RequestFormat = RestSharp.DataFormat.Json;        
        request.AddParameter("r", "releases");
        request.AddParameter("hasPom", "false");
        request.AddParameter("p", "zip");
        request.AddParameter("e", "zip");
        request.AddParameter("g", "otpp.devops");
        request.AddParameter("a", "githubSuspsendInactiveADAccounts");
        request.AddParameter("v", "1.0.1");
        //request.AddParameter("file", "@C:\\Temp\\Nexus\\githubSuspsendInactiveADAccounts-1.0.1.zip");

        string filename = "githubSuspsendInactiveADAccounts-1.0.1.zip";
        string filevalue = System.Convert.ToBase64String(RestSharpAPI.file_get_byte_contents(filename));
        request.AddParameter("file", filevalue);
        request.AddParameter("filename", filename);

        IRestResponse response = client.Execute<DFile>(request);
        Console.WriteLine("Response received: {0}", response.ResponseStatus);
    }

    /// <summary>
    /// Searches the metadata.
    /// </summary>
    /// <returns></returns>
    public List<NexusArtifact> searchMetadata()
    {
        RestRequest request = new RestRequest("/search/m2/ngfreeform", Method.GET);
        request.RequestFormat = RestSharp.DataFormat.Json;
        request.AddHeader("Accept", "application/json");
        request.AddParameter("p", "environment");
        request.AddParameter("t", "equal");
        request.AddParameter("v", "DEV");
        return this.restSharpAPI.Execute<List<NexusArtifact>>(this.client, request);
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

            //nexusAPI.uploadArtifact();
            //nexusAPI.uploadArtifact1();
            //nexusAPI.uploadArtifact2();
            //nexusAPI.uploadArtifact3();

            List<NexusArtifact> artifacts = new List<NexusArtifact>();
            artifacts = nexusAPI.searchMetadata();
            if (artifacts == null)
            {
                Console.WriteLine("artifacts is null");
                Console.ReadKey(true);
                Environment.Exit(1);
            }
            else {
                Console.WriteLine("Size of repos: {0}", artifacts.Count);                
            }

            Console.ReadKey(true);
        }
        catch (Exception ex)
        {
            Console.WriteLine("Exception caught: {0}", ex);
            Console.ReadKey(true);
        }
    }
}

