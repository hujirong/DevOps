namespace DevOps
{

    class UCDComponent
    {        
        public string id { get; set; }
        public string name { get; set; }

        public override string ToString()
        {
            string s = "id=" + id + " name=" + name;
            return s;
        }
    }

}