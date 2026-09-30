package ro.cortizo.panasoniccast;
import android.app.Activity;
import android.os.Bundle;
import android.os.Build;
import android.content.Intent;
import android.provider.Settings;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiManager;
import android.widget.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
 private TextView log; private Button scan;
 private final ExecutorService worker=Executors.newSingleThreadExecutor();
 private volatile DatagramSocket activeSocket;
 private void line(String s){runOnUiThread(()->log.append(s+"\n\n"));}
 @Override public void onCreate(Bundle b){super.onCreate(b);
  LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(24,24,24,24);
  TextView title=new TextView(this);title.setText("Panasonic Cast — diagnostic v0.1");title.setTextSize(22);root.addView(title);
  TextView info=new TextView(this);info.setText("Caută receptoare UPnP/DLNA. Nu transmite încă ecran sau sunet. Detectarea TV nu confirmă Miracast.");root.addView(info);
  Button cast=new Button(this);cast.setText("Deschide Cast Android");root.addView(cast);
  cast.setOnClickListener(v->{try {startActivity(new Intent(Settings.ACTION_CAST_SETTINGS));line("Ecran Cast deschis. Compatibilitatea trebuie testată manual.");}catch(Exception e){line("Ecran Cast inaccesibil: "+e.getClass().getSimpleName()+". Nu dovedește singur lipsa Miracast.");}});
  scan=new Button(this);scan.setText("Caută TV în Wi-Fi (8 secunde)");root.addView(scan);scan.setOnClickListener(v->discover());
  Button share=new Button(this);share.setText("Trimite diagnosticul");root.addView(share);share.setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_SEND);i.setType("text/plain");i.putExtra(Intent.EXTRA_TEXT,log.getText().toString());startActivity(Intent.createChooser(i,"Diagnostic"));});
  ScrollView scroll=new ScrollView(this);log=new TextView(this);log.setTextIsSelectable(true);scroll.addView(log);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);
  line("Telefon: "+Build.MANUFACTURER+" "+Build.MODEL+"; Android "+Build.VERSION.RELEASE);
  line("Cast settings declarat: "+(new Intent(Settings.ACTION_CAST_SETTINGS).resolveActivity(getPackageManager())!=null)+" (nu confirmă Miracast)");
 }
 private void discover(){
  ConnectivityManager cm=(ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
  Network wifi=null;for(Network n:cm.getAllNetworks()){NetworkCapabilities c=cm.getNetworkCapabilities(n);if(c!=null&&c.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)){wifi=n;break;}}
  if(wifi==null){line("Nu există rețea Wi-Fi conectată.");return;}
  final Network network=wifi;scan.setEnabled(false);line("Căutare SSDP pe Wi-Fi. TV trebuie pornit, în aceeași rețea.");
  worker.execute(()->{WifiManager.MulticastLock lock=null;
   try {WifiManager wm=(WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE);lock=wm.createMulticastLock("panasonic-discovery");lock.setReferenceCounted(false);lock.acquire();
    Set<String> seen=new HashSet<>();try(DatagramSocket socket=new DatagramSocket()){
     activeSocket=socket;network.bindSocket(socket);socket.setSoTimeout(700);
     String msg="M-SEARCH * HTTP/1.1\r\nHOST: 239.255.255.250:1900\r\nMAN: \"ssdp:discover\"\r\nMX: 2\r\nST: urn:schemas-upnp-org:device:MediaRenderer:1\r\n\r\n";
     byte[] bytes=msg.getBytes(StandardCharsets.US_ASCII);DatagramPacket request=new DatagramPacket(bytes,bytes.length,InetAddress.getByName("239.255.255.250"),1900);socket.send(request);socket.send(request);
     long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(8);
     while(System.nanoTime()<end&&!Thread.currentThread().isInterrupted()){byte[] buf=new byte[8192];DatagramPacket p=new DatagramPacket(buf,buf.length);try{socket.receive(p);}catch(SocketTimeoutException e){continue;}
      String response=new String(p.getData(),0,p.getLength(),StandardCharsets.UTF_8);String location="",server="";
      for(String h:response.split("\r?\n")){int colon=h.indexOf(':');if(colon<0)continue;String key=h.substring(0,colon).trim();String value=h.substring(colon+1).trim();if(key.equalsIgnoreCase("location"))location=value;if(key.equalsIgnoreCase("server"))server=value;}
      String id=p.getAddress().getHostAddress()+" "+location;if(seen.add(id))line("Receptor DLNA: "+p.getAddress().getHostAddress()+"\nServer: "+server+"\nDescriere: "+location+"\nIdentitatea Panasonic încă neconfirmată.");
     }
    }
    line(seen.isEmpty()?"Niciun receptor DLNA nu a răspuns. Posibile cauze: setări TV, izolare Wi-Fi sau SSDP blocat. Nu înseamnă că TV nu suportă DLNA.":"Căutare terminată. Receptoare: "+seen.size()+". Oglindirea nu este încă testată.");
   }catch(Exception e){line("Eroare căutare: "+e.getClass().getSimpleName()+": "+e.getMessage());}
   finally{activeSocket=null;if(lock!=null&&lock.isHeld())lock.release();runOnUiThread(()->scan.setEnabled(true));}
  });
 }
 @Override public void onDestroy(){DatagramSocket s=activeSocket;if(s!=null)s.close();worker.shutdownNow();super.onDestroy();}
}
