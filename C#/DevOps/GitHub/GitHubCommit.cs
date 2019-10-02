namespace DevOps
{

    class GitHubCommit
    {
        public string sha { get; set; }
        
        public override string ToString()
        {
            string s = "sha=" + sha;
            return s;
        }

        //Nested class
        class Commit
        {
            public string sha { get; set; }
            public string message { get; set; }

            public override string ToString()
            {
                string s = "sha=" + sha;
                return s;
            }
        }
    }

}