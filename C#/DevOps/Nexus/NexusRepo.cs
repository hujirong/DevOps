using RestSharp.Deserializers;

namespace DevOps
{
    /// <summary>
    /// Parse result from http://localhost:8060/nexus/service/local/all_repositories
    /// </summary>
    [DeserializeAs (Name = "repositories-item")]
    public class NexusRepo
    {
        public string id { get; set; }
        public string format { get; set; }
        public string resourceURI { get; set; }

        public override string ToString()
        {
            string s = "id=" + id + " format=" + format + " resourceURI=" + resourceURI;
            return s;
        }
    }

}