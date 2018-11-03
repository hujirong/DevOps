namespace DevOps
{
    // this may not be right
    class GitHubCommit
    {
        public string sha { get; set; }        
        public string message { get; set; }

        public override string ToString()
        {
            string s = "sha=" + sha + "message=" + message;
            return s;
        }
    }

}

