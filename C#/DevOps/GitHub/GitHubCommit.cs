namespace DevOps
{

    class GitHubCommit
    {
                public string id { get; set; }        

        public override string ToString()
        {
            string s = "id=" + id;
            return s;
        }
    }

}
