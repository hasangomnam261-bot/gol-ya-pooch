package ir.rayatalhoda.golyapooch;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Button;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.webkit.ValueCallback;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.util.Base64;
import android.widget.Toast;
import java.io.OutputStream;
import java.io.IOException;

public class MainActivity extends Activity {
  private WebView web;
  private ValueCallback<Uri[]> fileCallback;
  private static final int READ_FILE=101, WRITE_FILE=102;
  private byte[] pendingBytes;
  private String pendingMime;
  @Override public void onCreate(Bundle b){
    super.onCreate(b);
    LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(0xff08172b);
    LinearLayout nav=new LinearLayout(this);nav.setOrientation(LinearLayout.HORIZONTAL);
    Button manager=new Button(this);manager.setText("مدیر مسابقه");
    Button judge=new Button(this);judge.setText("ورود داور");
    nav.addView(manager,new LinearLayout.LayoutParams(0,-2,1));nav.addView(judge,new LinearLayout.LayoutParams(0,-2,1));
    root.addView(nav);web=new WebView(this);root.addView(web,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);
    WebSettings ws=web.getSettings();ws.setJavaScriptEnabled(true);ws.setDomStorageEnabled(true);ws.setAllowFileAccess(true);
    ws.setAllowFileAccessFromFileURLs(false);ws.setAllowUniversalAccessFromFileURLs(false);
    web.addJavascriptInterface(new FileBridge(),"AndroidFile");
    web.setWebViewClient(new WebViewClient());
    web.setWebChromeClient(new WebChromeClient(){
       @Override public boolean onShowFileChooser(WebView w,ValueCallback<Uri[]> cb,FileChooserParams p){
         if(fileCallback!=null)fileCallback.onReceiveValue(null);fileCallback=cb;
         Intent it=new Intent(Intent.ACTION_GET_CONTENT);it.setType("*/*");it.addCategory(Intent.CATEGORY_OPENABLE);
         startActivityForResult(Intent.createChooser(it,"انتخاب فایل"),READ_FILE);return true;
       }
    });
    manager.setOnClickListener(v->web.loadUrl("file:///android_asset/manager.html"));
    judge.setOnClickListener(v->web.loadUrl("file:///android_asset/judge.html"));
    web.loadUrl("file:///android_asset/manager.html");
  }
  public class FileBridge{
    @JavascriptInterface public void save(String filename,String mime,String payload){
      try{
        final byte[] data=Base64.decode(payload,Base64.DEFAULT);
        runOnUiThread(()->{
          pendingBytes=data;pendingMime=mime;
          Intent it=new Intent(Intent.ACTION_CREATE_DOCUMENT);it.addCategory(Intent.CATEGORY_OPENABLE);
          it.setType(mime);it.putExtra(Intent.EXTRA_TITLE,filename);
          startActivityForResult(it,WRITE_FILE);
        });
      }catch(Exception e){runOnUiThread(()->Toast.makeText(MainActivity.this,"خطا در آماده‌سازی فایل",Toast.LENGTH_LONG).show());}
    }
  }
  @Override protected void onActivityResult(int request,int result,Intent data){
    super.onActivityResult(request,result,data);
    if(request==READ_FILE){if(fileCallback!=null){fileCallback.onReceiveValue(result==RESULT_OK&&data!=null&&data.getData()!=null?new Uri[]{data.getData()}:null);fileCallback=null;}return;}
    if(request==WRITE_FILE&&result==RESULT_OK&&data!=null&&data.getData()!=null&&pendingBytes!=null){
      try(OutputStream out=getContentResolver().openOutputStream(data.getData())){
        if(out!=null)out.write(pendingBytes);
        Toast.makeText(this,"فایل ذخیره شد",Toast.LENGTH_SHORT).show();
      }catch(IOException e){Toast.makeText(this,"ذخیره فایل ناموفق بود",Toast.LENGTH_LONG).show();}
    }
    pendingBytes=null;
  }
  @Override public void onBackPressed(){if(web!=null&&web.canGoBack())web.goBack();else super.onBackPressed();}
  @Override protected void onDestroy(){if(web!=null)web.destroy();super.onDestroy();}
}
