import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;


// Author @ roottraveller
// date : Oct 2017

class MyServer {
	ArrayList alist = new ArrayList();
	ArrayList users = new ArrayList();
	ServerSocket serversocket = null;   //java.net.ServerSocket
	Socket socket = null;

	public final static int PORT = 1037; //Port no (0-1023) are reserved for standard services, such as email, FTP, and HTTP.
	public final static String UPDATE_USERS="updateuserslist:";
	public final static String LOGOUT_MESSAGE="@@logoutme@@:";
	
	public MyServer() {
		try{
			serversocket = new ServerSocket(PORT);
			System.out.println("Server Started " + serversocket);
			
			while(true) {
				socket = serversocket.accept();
				Runnable r = new MyThread(socket, alist, users);
				Thread t = new Thread(r);
				t.start();
				//	System.out.println("Total alive clients : " + serversocket);
			}
		} catch(Exception e) {
			System.err.println("Server constructor : " + e);
		}
	}

	/*public static void main(String [] args) {
		new MyServer();
	}*/
} // Class ends


class MyThread implements Runnable {
	Socket socket = null;
	ArrayList alist = null;
	ArrayList users = null;
	String username;

	MyThread (Socket socket, ArrayList alist, ArrayList users){
		this.socket = socket;
		this.alist = alist;
		this.users = users;
		
		try {
			DataInputStream dis = new DataInputStream(socket.getInputStream());
			username = dis.readUTF();
			
			alist.add(socket);
			users.add(username);
			
			tellEveryOne("  ****** " + username + " Logged In at " + (new Date()) + " ******  ");
			sendNewUserList();
		} catch(Exception e) {
			System.err.println("MyThread constructor : "+e);
		}
	}

	public void run() {
		String msg;
		
		try {
			DataInputStream dis = new DataInputStream(socket.getInputStream());
			
			do {
				msg = dis.readUTF();
				if(msg.toLowerCase().equals(MyServer.LOGOUT_MESSAGE)) 
					break;
					
				//	System.out.println("received from " + socket.getPort());
				tellEveryOne(username + " : " + msg);
			} while(true);
			
			// Logout process started
			DataOutputStream tdos = new DataOutputStream(socket.getOutputStream());
			tdos.writeUTF(MyServer.LOGOUT_MESSAGE);
			tdos.flush();
			
			tellEveryOne("  ****** " + username + " Logged out at " + (new Date()) + " ******");
			users.remove(username);
			sendNewUserList();
			alist.remove(socket);
			socket.close();
		   } catch(Exception e) { 
		       System.out.println("MyThread Run : "+e);
		   }
	}

	public void sendNewUserList() {
		tellEveryOne(MyServer.UPDATE_USERS + users.toString());
	}

	public void tellEveryOne(String msg)	{
		Iterator i = alist.iterator();
		
		while(i.hasNext()) {
			try {
				Socket temp = (Socket)i.next();
				DataOutputStream dos = new DataOutputStream(temp.getOutputStream());
				dos.writeUTF(msg);
				dos.flush();
				//System.out.println("sent to : "+temp.getPort()+"  : "+ s1);
			} catch(Exception e) { 
			   	System.err.println("TellEveryOne "+e);
			}
		}
	}
} // class ends


class MyClient implements ActionListener {
	Socket socket = null;
	DataInputStream dis = null;
	DataOutputStream dos = null;

	JButton sendButton, logoutButton, loginButton, exitButton;
	JFrame chatWindow;
	JTextArea txtBroadcast;
	JTextArea txtMessage;
	JList usersList;

	public MyClient() {
	  	displayGUI();
	  	new MyServer();
		// clientChat();
	}

	private void displayGUI() {
		chatWindow = new JFrame();
		txtBroadcast = new JTextArea(10,50);
		txtBroadcast.setEditable(false);
		txtMessage = new JTextArea(5,30);
		usersList = new JList();

		sendButton = new JButton("Send");
		logoutButton = new JButton("Log out");
		loginButton = new JButton("Log in");
		exitButton = new JButton("Exit");

		JPanel center1 = new JPanel();
		center1.setLayout(new BorderLayout());
		center1.add(new JLabel("BroadCast messages from all online users", JLabel.CENTER), "North");
		center1.add(new JScrollPane(txtBroadcast), "Center");

		JPanel south1 = new JPanel();
		south1.setLayout(new FlowLayout());
		south1.add(new JScrollPane(txtMessage));
		south1.add(sendButton);

		JPanel south2 = new JPanel();
		south2.setLayout(new FlowLayout());
		south2.add(loginButton);
		south2.add(logoutButton);
		south2.add(exitButton);

		JPanel south = new JPanel();
		south.setLayout(new GridLayout(2,1));
		south.add(south1);
		south.add(south2);

		JPanel east = new JPanel();
		east.setLayout(new BorderLayout());
		east.add(new JLabel("Online Users", JLabel.CENTER), "East");
		east.add(new JScrollPane(usersList), "South");

		chatWindow.add(east,"East");

		chatWindow.add(center1, "Center");
		chatWindow.add(south, "South");

		chatWindow.pack();
		chatWindow.setTitle("Login to Chat");
		chatWindow.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		chatWindow.setVisible(true);
		
		sendButton.addActionListener(this);
		logoutButton.addActionListener(this);
		loginButton.addActionListener(this);
		exitButton.addActionListener(this);
		
		logoutButton.setEnabled(false);
		loginButton.setEnabled(true);
		
		txtMessage.addFocusListener(new FocusAdapter() {
			public void focusGained(FocusEvent fe) {
				txtMessage.selectAll();
			}
		});

		chatWindow.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent ev) {
				if(socket != null) {
					JOptionPane.showMessageDialog(chatWindow,"you are logged out now. ", "Exit", JOptionPane.INFORMATION_MESSAGE);
					logoutSession();
				}
				System.exit(0);
			}
		});
	}

	public void actionPerformed(ActionEvent ev) {
		JButton temp = (JButton)ev.getSource();
		
		if(temp == sendButton) {
			if(socket == null) {
		 		JOptionPane.showMessageDialog(chatWindow,"You are not logged in. Login first"); 
		 		return;
		 	}
		 	
			try {
				dos.writeUTF(txtMessage.getText());
				txtMessage.setText("");
			} catch(Exception e) { 
				txtBroadcast.append("\nsend button click : " + e);
			}
		}
		
		if(temp == loginButton) {
			String uname = JOptionPane.showInputDialog(chatWindow,"Enter Your username / nickname: ");
			
			if(uname != null)
				clientChat(uname); 
		}
		
		if(temp == logoutButton) {
			if(socket != null) {
				JOptionPane.showMessageDialog(chatWindow,"you are logged out now. ","Exit", JOptionPane.INFORMATION_MESSAGE);
				logoutSession();
			}
		}
		
		if(temp == exitButton) {
			if(socket != null) {
				JOptionPane.showMessageDialog(chatWindow,"you are logged out now. ","Exit", JOptionPane.INFORMATION_MESSAGE);
				logoutSession();
			}
			System.exit(0);
		}
	}

	public void logoutSession() {
		if(socket == null) 
			return;
		
		try {
			dos.writeUTF(MyServer.LOGOUT_MESSAGE);
			Thread.sleep(500);
			socket = null;
		} catch(Exception e) { 
			txtBroadcast.append("\n inside logoutSession Method : "+e);
		}

		logoutButton.setEnabled(false);
		loginButton.setEnabled(true);
		chatWindow.setTitle("Login for Chat");
	}

	public void clientChat(String uname) {
		try {
			 socket = new Socket(InetAddress.getLocalHost(), MyServer.PORT);
			 dis = new DataInputStream(socket.getInputStream());
			 dos = new DataOutputStream(socket.getOutputStream());
			 
			 ClientThread ct = new ClientThread(dis, this);
			 Thread t1 = new Thread(ct);
			 t1.start();
			 
			 dos.writeUTF(uname);
			 chatWindow.setTitle(uname + " Chat Window");
		} catch(Exception e) { 
			txtBroadcast.append("\nClient Constructor : " + e);
		}
		
		logoutButton.setEnabled(true);
		loginButton.setEnabled(false);
	}

	

	public static void main(String[] args) {
		new MyClient();
	}
} //Class ends


class ClientThread implements Runnable {
	DataInputStream dis = null;
	MyClient client = null;

	ClientThread(DataInputStream dis, MyClient client) {
		this.dis = dis;
		this.client = client;
	}

	public void run() {
		String msg = "";
		
		do {
			try {
				msg = dis.readUTF();
				if(msg.startsWith(MyServer.UPDATE_USERS)) {
					updateUsersList(msg);
				} else if(msg.equals(MyServer.LOGOUT_MESSAGE)) {
					break;
				} else {
					client.txtBroadcast.append("\n" + msg);
				}
				
				int lineOffset = client.txtBroadcast.getLineStartOffset(client.txtBroadcast.getLineCount()-1);
				client.txtBroadcast.setCaretPosition(lineOffset);
			} catch(Exception e) { 
				client.txtBroadcast.append("\nClientThread run : " + e);
			}
	   } while(true);
	}

	public void updateUsersList(String ul) 	{
		Vector ulist = new Vector();

		ul = ul.replace("[", "");
		ul = ul.replace("]", "");
		ul = ul.replace(MyServer.UPDATE_USERS, "");
		StringTokenizer st = new StringTokenizer(ul, ",");

		while(st.hasMoreTokens()) {
			String temp = st.nextToken();
			ulist.add(temp);
		}
		
		client.usersList.setListData(ulist);
	}
} //Class ends
