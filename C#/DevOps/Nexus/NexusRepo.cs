using RestSharp.Deserializers;

namespace DevOps
{
    [DeserializeAs (Name = "repositories-item")]
    public class NexusRepo
    {
        public string name { get; set; }
        public string format { get; set; }
        public string resourceURI { get; set; }

        public override string ToString()
        {
            string s = "name=" + name + " format=" + format + " resourceURI=" + resourceURI;
            return s;
        }
    }

}