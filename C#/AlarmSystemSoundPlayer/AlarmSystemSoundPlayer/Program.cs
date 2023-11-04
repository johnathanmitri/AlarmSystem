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
            
            System.Media.SoundPlayer DingPlayer = new System.Media.SoundPlayer(AppDomain.CurrentDomain.BaseDirectory + @"DingSound.wav");
            DingPlayer.PlaySync();

            System.Media.SoundPlayer namePlayer = new System.Media.SoundPlayer();
            while (true)
            {
                try
                {
                    namePlayer.SoundLocation = AppDomain.CurrentDomain.BaseDirectory + Console.ReadLine();

                    DingPlayer.PlaySync();

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
