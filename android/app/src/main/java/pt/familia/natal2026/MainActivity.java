package pt.familia.natal2026;
import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.webkit.ValueCallback;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
public class MainActivity extends Activity {
    private static final int EXPORT=41, IMPORT=42, PHOTO_PICK=43;
    private WebView web;
    private String exportJson;
    private ValueCallback<Uri[]> photoCallback;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        web=new WebView(this);
        web.setWebViewClient(new WebViewClient());
        web.setWebChromeClient(new WebChromeClient(){
            @Override public boolean onShowFileChooser(WebView view,ValueCallback<Uri[]> callback,FileChooserParams params){
                if(photoCallback!=null)photoCallback.onReceiveValue(null);
                photoCallback=callback;
                try {Intent i=params.createIntent();startActivityForResult(i,PHOTO_PICK);return true;}
                catch(Exception ex){photoCallback=null;callback.onReceiveValue(null);return false;}
            }
        });
        WebSettings settings=web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        web.addJavascriptInterface(new Bridge(),"Android");
        setContentView(web);
        web.loadUrl("file:///android_asset/index.html");
    }
    public class Bridge {
        @JavascriptInterface public void exportData(String json){
            runOnUiThread(()->{exportJson=json;Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/json");i.putExtra(Intent.EXTRA_TITLE,"presentes-natal-2026.json");startActivityForResult(i,EXPORT);});
        }
        @JavascriptInterface public void importData(){
            runOnUiThread(()->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/json");startActivityForResult(i,IMPORT);});
        }
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request==PHOTO_PICK){if(photoCallback!=null){photoCallback.onReceiveValue(result==RESULT_OK&&data!=null&&data.getData()!=null?new Uri[]{data.getData()}:null);photoCallback=null;}return;}
        if(result!=RESULT_OK||data==null||data.getData()==null)return;
        Uri uri=data.getData();
        try{
            if(request==EXPORT){try(OutputStream out=getContentResolver().openOutputStream(uri)){out.write(exportJson.getBytes(StandardCharsets.UTF_8));}}
            else if(request==IMPORT){String json;try(InputStream in=getContentResolver().openInputStream(uri)){java.io.ByteArrayOutputStream b=new java.io.ByteArrayOutputStream();byte[] chunk=new byte[4096];int n;while((n=in.read(chunk))!=-1)b.write(chunk,0,n);json=b.toString("UTF-8");}String encoded=android.util.Base64.encodeToString(json.getBytes(StandardCharsets.UTF_8),android.util.Base64.NO_WRAP);web.evaluateJavascript("window.importNatalData(decodeURIComponent(escape(atob('"+encoded+"'))))",null);}
        }catch(Exception e){android.widget.Toast.makeText(this,"Erro ao ler ou guardar ficheiro",android.widget.Toast.LENGTH_LONG).show();}
    }
    @Override public void onBackPressed(){if(web.canGoBack())web.goBack();else super.onBackPressed();}
}
