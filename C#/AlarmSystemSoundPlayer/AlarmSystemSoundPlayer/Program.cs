using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace AlarmSystemSoundPlayer
{
    internal class Program
    {
        static void Main(string[] args)
        {
            
            System.Media.SoundPlayer DooDooPlayer = new System.Media.SoundPlayer(AppDomain.CurrentDomain.BaseDirectory + @"DooDooSound.wav");
            DooDooPlayer.PlaySync();

            System.Media.SoundPlayer namePlayer = new System.Media.SoundPlayer();
            while (true)
            {
                try
                {
                    namePlayer.SoundLocation = AppDomain.CurrentDomain.BaseDirectory + Console.ReadLine();

                    DooDooPlayer.PlaySync();

                    namePlayer.PlaySync();         
                }
                catch
                {
                    Console.WriteLine("Sound does not exist.");
                }
            }
        }
    }
}
