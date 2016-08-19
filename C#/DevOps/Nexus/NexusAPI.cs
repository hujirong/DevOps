using System;
using System.IO;
using RestSharp;
using System.Collections.Generic;
using System.Text.RegularExpressions;
using DevOps;

/// <summary>
/// https://github.com/restsharp/RestSharp/wiki/Recommended-Usage
/// </summary>
class NexusAPI
{
    RestClient client;
    RestSharpAPI restSharpAPI;
    //public static string baseUrl = "http://localhost:8060/nexus/service/local";
    public static string baseUrl = "http://invnexus.dev.otpp.com//nexus/service/local";
    //static string user = "admin";
    //static string password = "admin123";

    static string user = "local_admin";
    static string password = "password";

    List<string> urls = new List<string>(); // foler URL to be deleted

    /// <summary>
    /// Initializes a new instance of the <see cref="GitHubAPI"/> class.
    /// </summary>
    /// <param name="client">The RestClient.</param>
    public NexusAPI(RestSharpAPI restSharpAPI, RestClient client) {
        this.restSharpAPI = restSharpAPI;
        this.client = client;
    }

    /// <summary>    
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
    /// http://localhost:8060/nexus/nexus-custom-metadata-plugin/m2/docs/path__search_m2_freeform.html
    /// http://localhost:8060/nexus/service/local/search/m2/ngfreeform?p=environment&t=equal&v=DEV
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

    /// <summary>
    /// http://localhost:8060/nexus/service/local/repositories/thirdparty/content/    
    /// </summary>
    /// <returns></returns>
    public List<NexusContent> getContent(string repo, string path)
    {
        RestRequest request = new RestRequest("repositories/" + repo + "/content" + path, Method.GET);

        return this.restSharpAPI.Execute<List<NexusContent>>(this.client, request);        
    }


    /// <summary>
    /// Searches the artifact.
    /// https://repository.sonatype.org/nexus-indexer-lucene-plugin/default/docs/path__lucene_search.html
    /// http://stackoverflow.com/questions/24957270/nexus-rest-api-query-artifacts-within-a-group
    /// </summary>
    /// <param name="repo">The repo.</param>
    /// <param name="group">The group.</param>
    /// <param name="artifact">The artifact.</param>
    /// <returns></returns>
    public List<NexusArtifact> searchArtifact(string repo, string group, string artifact)
    {
        RestRequest request = new RestRequest("lucene/search?r=" + repo + "&g=" + group + "&a=" + artifact, Method.GET);

        return this.restSharpAPI.Execute<List<NexusArtifact>>(this.client, request);
    }

    /// <summary>
    /// Deletes the content folder will delete everything inside.
    /// </summary>
    /// <param name="path">The path.</param>
    /// <returns></returns>
    public IRestResponse deleteContent(string path)
    {
        RestRequest request = new RestRequest(path, Method.DELETE);
        IRestResponse response = this.restSharpAPI.Execute(this.client, request);        
        return response;
    }

    /// <summary>
    /// Finds the artifacts to be deleted based on days and pattern
    /// </summary>
    /// <param name="repository">The repository.</param>
    /// <param name="contents">The contents.</param>
    /// <param name="days">The days.</param>
    
    public void findArtifacts(string repository, List<NexusContent> contents, int days, string pattern)
    {
        // List<string> urls = new List<string>(); // store all URLs to be deleted, moved to class level
        string folderURL;
        foreach (NexusContent cont in contents)
        {            
            if (cont.text.EndsWith(pattern))
            {
                //Console.WriteLine("Find the .zip file: {0}", cont.resourceURI);
                if (isOlderThan(cont.lastModified, days))
                {
                    //Console.WriteLine("It's a file, older than: {0} days: {1}", days, cont.resourceURI);
                    folderURL = cont.resourceURI.Remove(cont.resourceURI.Length - cont.text.Length);
                    //Console.WriteLine("folderURL: {0}", folderURL);

                    // add the parent folder of the found artifact, to be deleted
                    urls.Add(folderURL);
                }
            }

            if (cont.leaf == "false")
            {
                //Console.WriteLine("It's a directory, search under.");
                findArtifacts(repository, getContent(repository, cont.relativePath), days, pattern);
            }
        }       
        
    }

    /// <summary>
    /// Determines whether [is older than] [the specified last modified].
    /// </summary>
    /// <param name="lastModified">The last modified.</param>
    /// <param name="days">The days.</param>
    /// <returns></returns>
    public bool isOlderThan (string lastModified, int days)
    {     
        //<lastModified>2016-08-11 14:12:26.37 UTC</lastModified>
        //string lastModified = "2016-08-11 14:12:26.37 UTC";
        //DateTime artifactDate = DateTime.ParseExact(lastModified, "yyyy-MM-dd HH:mm:ss.fff UTC", System.Globalization.CultureInfo.InvariantCulture);
        string lastModifiedDate = lastModified.Substring(0, 10);  // fff, or ff, or f, not consistent
        //Console.WriteLine("lastModifiedDate: {0}", lastModifiedDate);

        DateTime artifactDate = DateTime.ParseExact(lastModifiedDate, "yyyy-MM-dd", System.Globalization.CultureInfo.InvariantCulture);
        //Console.WriteLine("diff: {0}", (DateTime.Now - artifactDate).TotalDays);
        return (DateTime.Now - artifactDate).TotalDays > days;        
    }

    /// <summary>
    /// Removes the last version from list, so the final list can be deleted
    /// </summary>
    /// <param name="repo">The repo.</param>
    public void removeLastFromList(string repo)
    {    
        for (int i = urls.Count - 1; i >= 0; i--)
        {
            Console.WriteLine("\n");
            Console.WriteLine("folderURL: {0}", urls[i]);

            string[] ss = urls[i].Split('/');
            int size = ss.Length;
            string version = ss[size - 2];
            string artifact = ss[size - 3];
            Console.WriteLine("artifact={0}", artifact);
            Console.WriteLine("version={0}", version);

            string temp = urls[i].TrimEnd('/');
            //Console.WriteLine("after trim:{0}", temp);
            int content = urls[i].IndexOf("content");
            // up to the group id
            temp = urls[i].Remove((urls[i].Length - (version.Length + artifact.Length) - 2), version.Length + artifact.Length + 2);
            //Console.WriteLine("after remove artifact and version: {0}", temp);
            string group = temp.Substring(content + 7);
            //Console.WriteLine("group: {0}", group);
            group = group.TrimEnd('/');
            group = group.TrimStart('/');
            group = group.Replace('/', '.');
            Console.WriteLine("group: {0}", group);            

            List<NexusArtifact> nexusArtifacts = new List<NexusArtifact>();
            nexusArtifacts = searchArtifact(repo, group, artifact);

            if (nexusArtifacts == null)
            {
                Console.WriteLine("Search error, nexusArtifacts is null");
                Environment.Exit(1);
            }
       
            string latestRelease = nexusArtifacts[0].latestRelease;
            Console.WriteLine("latestRelease: {0}", latestRelease);
                       
            if (latestRelease == version)
            {
                 Console.WriteLine("latest version of: {0} {1} {2}", group, artifact, version);
                 urls.Remove(urls[i]);
            }            
        }        
    }
    
    public void deleteArtifact(List<string> urls, string repo)
    {
        IRestResponse response = null;
        foreach (string url in urls)
        {            
            Console.WriteLine("URL to be deleted: {0}", url);
            //Delete artifact
            response = deleteContent(url);
            Console.WriteLine("Response: {0}", response.ResponseStatus);

            //Rebuild metadata
            string metadataURL = NexusAPI.baseUrl + "/service/local/metadata/repositories/" + repo + "/content/";
            response = deleteContent(metadataURL);
            Console.WriteLine("Response: {0}", response.ResponseStatus);
        }
    }

    public void testDelete()
    {        
        IRestResponse response = deleteContent("repositories/otpp-snapshots/content/OTPP/devops/README.md/1.0.0-rc1/");
        Console.WriteLine("Response: {0}", response.ResponseStatus);
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
                Environment.Exit(1);
            }

            NexusAPI nexusAPI = new NexusAPI(restSharpAPI, client);

            //nexusAPI.testDelete();
            //Console.ReadKey(true);

            List<NexusContent> contents = new List<NexusContent>();
            string repository = "otpp-snapshots";
            //string repository = "releases";
            string pattern = ".zip";
            int days = 0;
            contents = nexusAPI.getContent(repository, "/");
                        
            nexusAPI.findArtifacts(repository, contents, days, pattern);            
            Console.WriteLine("Size of artifacts: {0}", nexusAPI.urls.Count);
            
            nexusAPI.removeLastFromList(repository);

            
            for (int i = nexusAPI.urls.Count - 1; i >= 0; i--)
            {
                nexusAPI.urls[i].TrimEnd('/');
                int local = nexusAPI.urls[i].IndexOf("/local/");
                nexusAPI.urls[i] = nexusAPI.urls[i].Substring(local + 6);
                Console.WriteLine("URL to be deleted: {0}", nexusAPI.urls[i]);
            }

            //nexusAPI.deleteArtifact(nexusAPI.urls, repository);
            

            Console.ReadKey(true);



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
               // Console.WriteLine("Repo info: {0}", repo.ToString());
            }

            //nexusAPI.uploadArtifact();
            //nexusAPI.uploadArtifact1();
            //nexusAPI.uploadArtifact2();
            //nexusAPI.uploadArtifact3();
            
            //List<NexusArtifact> artifacts = new List<NexusArtifact>();
            //artifacts = nexusAPI.searchMetadata();
            //if (artifacts == null)
            //{
            //    Console.WriteLine("artifacts is null");
            //    Console.ReadKey(true);
            //    Environment.Exit(1);
            //}
            //else {
            //    Console.WriteLine("Size of repos: {0}", artifacts.Count);                
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

