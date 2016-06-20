using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Security;

class UpdatePassword
{
    UpdatePassword() { }

    static void Main (string[] args)
    {
        string password = "";
        string encryptedPassword, decryptedPassword;
        encryptedPassword = DPAPICipher.EncryptString(DPAPICipher.ToSecureString(password));
        Console.WriteLine("encryptedPassword={0}", encryptedPassword);

        SecureString secret = DPAPICipher.DecryptString(encryptedPassword);
        Console.WriteLine("decryptedPassword={0}", secret.ToString());

        
        System.Console.ReadKey(true);
        ;
    }
}

