using RestSharp.Deserializers;

namespace DevOps
{
    /// <summary>
    /// This is a copy of NextArtifacts.cs, just to make the build happy
    /// </summary>
    [DeserializeAs(Name = "artifact")]
    class NexusContent
    {
        public string leaf { get; set; }
        public string text { get; set; }
        public string lastModified { get; set; }
        public string resourceURI { get; set; }
        public string relativePath { get; set; }

        public override string ToString()
        {
            string s = "text=" + text + " lastModified=" + lastModified + " resourceURI=" + resourceURI + " relativePath=" + relativePath;
            return s;
        }
    }
}
