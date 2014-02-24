using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Drawing;
using System.Text;
using System.Windows.Forms;
using System.Net.Sockets;
using System.Net;
using System.Runtime.InteropServices;

namespace SPenClient
{
    public partial class FormMain : Form
    {
        public class PenData
        {
            public PenData(FormMain form)
            {
                this.form = form;
            }

            public float x;
            public float y;
            public float pressure;
            public int action;
            public string type;
            public int index;
            public string up;

            public float _x;
            public float _y;
            public float _pressure;
            public int _action;
            public string _type;
            public int _index;
            public string _up;

            private string[] dta;

            public bool isNew =true;
            private FormMain form;

            public void SetData(object raw)
            {
                LoadData(raw);

                if (isNew)
                {
                    isNew = false;
                    SetBackup();
                }

                if (!type.Equals(_type))
                {
                    this._index = this.index;
                }

                if (this.index >= this._index)
                {

                    if (!type.Equals(_type))
                    {
                        if (type.Equals("hover") && _type.Equals("pen"))
                        {
                            mouse_event((int)(MouseEventFlags.LEFTUP), 0, 0, 0, 0);
                        }

                        if (type.Equals("pen") && _type.Equals("hover"))
                        {
                            mouse_event((int)(MouseEventFlags.LEFTDOWN), 0, 0, 0, 0);
                        }
                    }


                    if (pressure <= 0 && Math.Min(x, y) <= 0 & type.Equals("hover") || type.Equals("finger") && up.Equals("up"))
                    {
                        mouse_event((int)(MouseEventFlags.LEFTUP), 0, 0, 0, 0);
                        isNew = true;
                        RestoreBackup();
                        return;
                    }

                    Cursor.Position = new System.Drawing.Point(this.GetX, this.GetY);

                    if (form.WindowState == FormWindowState.Normal)
                    {
                        form.labelx.Text = x.ToString();
                        form.labely.Text = y.ToString();
                        form.labelp.Text = pressure.ToString();
                        form.labela.Text = action.ToString();
                        form.labelt.Text = type.ToString();
                        form.labeli.Text = index.ToString();
                        form.labelu.Text = up.ToString();
                    }

                    SetBackup();
                }
            }

            private void LoadData(object raw)
            {
                string sep = System.Globalization.CultureInfo.CurrentCulture.NumberFormat.NumberDecimalSeparator;
                string str = ((string)raw).Replace(",", sep).Replace(".", sep);
                this.dta = (str).Split(new char[] { '|' });

                this.x = float.Parse(this.dta[0]);
                this.y = float.Parse(this.dta[1]);
                this.pressure = float.Parse(this.dta[2]);
                this.action = int.Parse(this.dta[3]);
                this.type = this.dta[4];
                this.index = int.Parse(this.dta[5]);
                this.up = this.dta[6];
            }


            public void SetBackup()
            {
                this._x = this.x;
                this._y = this.y;
                this._pressure = this.pressure;
                this._action = this.action;
                this._type = this.type;
                this._index = this.index;
                this._up = this.up;
            }

            public void RestoreBackup()
            {
                this.x = this._x;
                this.y = this._y;
                this.pressure = this._pressure;
                this.action = this._action;
                this.type = this._type;
                this.index = this._index;
                this.up = this._up;
            }

            public int GetX { 
                get {
                    return Cursor.Position.X + (int)(x - _x);
                } 
            }

            public int GetY { 
                get {
                    return Cursor.Position.Y + (int)(y - _y);
                } 
            }
        }

        private PenData pen;
        BackgroundWorker bw;
        public FormMain()
        {
            InitializeComponent();

            pen = new PenData(this);

            init(12333);
        }

        private void init(int port)
        {
            bw = new BackgroundWorker();
            bw.WorkerReportsProgress = true;
            bw.WorkerSupportsCancellation = true;
            bw.DoWork += new DoWorkEventHandler(bw_DoWork);
            bw.ProgressChanged += new ProgressChangedEventHandler(bw_ProgressChanged);
            bw.RunWorkerAsync(port);
        }

        void bw_ProgressChanged(object sender, ProgressChangedEventArgs e)
        {
            this.pen.SetData(e.UserState);
        }

        void bw_DoWork(object sender, DoWorkEventArgs e)
        {
            UdpClient udpClient = new UdpClient((int)e.Argument);
            IPEndPoint groupEP = new IPEndPoint(IPAddress.Any, (int)e.Argument);
            BackgroundWorker worker = sender as BackgroundWorker;
            do
            {
                byte[] receiveBytes = udpClient.Receive(ref groupEP);
                string returnData = Encoding.ASCII.GetString(receiveBytes);
                worker.ReportProgress(0, returnData);
            } while (!worker.CancellationPending);
        }

        [DllImport("user32.dll")]
        static extern void mouse_event(int dwFlags, int dx, int dy, int dwData, int dwExtraInfo);

        [Flags]
        public enum MouseEventFlags
        {
            LEFTDOWN = 0x00000002,
            LEFTUP = 0x00000004,
            MIDDLEDOWN = 0x00000020,
            MIDDLEUP = 0x00000040,
            MOVE = 0x00000001,
            ABSOLUTE = 0x00008000,
            RIGHTDOWN = 0x00000008,
            RIGHTUP = 0x00000010
        }

        private void button1_Click(object sender, EventArgs e)
        {
            bw.CancelAsync();
            bw.Dispose();
            init(int.Parse(textBox1.Text));
        }
    }
}