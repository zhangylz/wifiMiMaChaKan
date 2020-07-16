package cn.kaicity.apps.wifikeylook;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.*;
import android.content.*;
import android.os.*;
import android.util.*;
import android.view.*;
import android.widget.*;
import android.widget.AdapterView.*;
import cn.kaicity.apps.utils.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;
import org.xmlpull.v1.*;

public class MainActivity extends Activity 
{
	private ListView lt;

    @SuppressLint("WrongConstant")
	@Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
		lt = findViewById(R.id.lt);
		String path="";
		if(Build.VERSION.SDK_INT>25){
			path = "/data/misc/wifi/WifiConfigStore.xml";
		}else{
			path = "/data/misc/wifi/*.conf";
			
		}
		String xml=Utils.getPermission(path);
		
		if(xml==""){
			Toast.makeText(this,"读取文件失败,可能是没有root权限",0).show();
		}else if(Build.VERSION.SDK_INT>25){
			Reader read=new StringReader(xml);
			try
			{
				decodeXml(read);
			}
			catch (IOException e)
			{
				Toast.makeText(this,"出错"+e,0).show();
			}
			catch (XmlPullParserException e)
			{
				Toast.makeText(this,"xml解析出错,可能是文件已将加密"+e,0).show();
				
			}
		}else{
			decodeConf(xml);
		}
    }
	
	private void decodeConf(String str) {
		List<Map<String,Object>> arrayList = new ArrayList<>();
        Matcher matcher = Pattern.compile("network=\\{([^\\}]+)\\}", 32).matcher(str);
        while (matcher.find()) {
            String group = matcher.group();
            Matcher hash = Pattern.compile("ssid=\"([^\"]+)\"").matcher(group);
            Matcher text = Pattern.compile("ssid=[a-fA-F0-9]+").matcher(group);
            Matcher key;
            if (hash.find()) {
                HashMap<String,Object> map = new HashMap<>();
                map.put("name", hash.group(1));
				key = Pattern.compile("psk=\"([^\"]+)\"").matcher(group);
                if (key.find()) {
					map.put("name","名称:"+hash.group(1));
                    map.put("key", String.format("密码:%s", key.group(1)));
                    map.put("truekey",key.group(1));
                    arrayList.add(map);
                }
            } else if (text.find()) {
				HashMap<String,Object> map = new HashMap<>();
               	key = Pattern.compile("psk=\"([^\"]+)\"").matcher(group);
                if (key.find()) {
					map.put("name", "名称:"+decodeSSID(group));
                    map.put("key", String.format("密码:%s", key.group(1)));
                    map.put("truekey", key.group(1));
                    arrayList.add(map);
                }
            }
        }
        show(arrayList);
    }

	private String decodeSSID(String str) {
		String decode = URLDecoder.decode(str.substring(str.indexOf("configKey") + 9, str.lastIndexOf("-")));
        decode = decode.substring(5, decode.length());
        return decode.substring(0, decode.length() - 2);
    }
	
	private void decodeXml(Reader read) throws XmlPullParserException, FileNotFoundException, IOException
	{
		List<Map<String,Object>> list=new ArrayList<>();
		HashMap<String,Object> map=new HashMap<>();
		
		XmlPullParser pull=Xml.newPullParser();
		pull.setInput(read);
		int eventType=pull.getEventType();

		while (eventType != pull.END_DOCUMENT)
		{        
            String nodeName=pull.getName();     
			
            switch (eventType)
			{          
				case XmlPullParser.START_TAG:                                
					if ("string".equals(nodeName))
					{    
					
						if ("SSID".equals(pull.getAttributeValue(null, "name")))
						{
							pull.next();               
							String text=pull.getText().trim();
							text=text.substring(1,text.length()-1);
							map.put("name",String.format("名称:%s",text));
							if(map.size()==3){
								list.add(map);
								map=new HashMap<>();
							}
						}  
						if ("PreSharedKey".equals(pull.getAttributeValue(null, "name")))
						{
							pull.next();               
							String text=pull.getText().trim();   
							text=text.substring(1,text.length()-1);
							map.put("key",String.format("密码:%s",text));
							map.put("truekey",text);
							if(map.size()==3){
								list.add(map);
								map=new HashMap<>();
							}
							
						}
					}
					break;
				default:               
					break;       
			}   
			eventType = pull.next();    
            //打印出每次解析eventType变化
		}
		show(list);
	}

	private void show(final List<Map<String,Object>> list)
	{
		if(list.size()==0){
			new AlertDialog.Builder(this)
			.setTitle("提示")
			.setMessage("获取到连接过的wifi及密码为空,可能是由于没有权限，请重新授权。当然，也有可能是你一个wifi都没连接过\nʘᴗʘ")
			.setPositiveButton("确定",null)
			.show();
		}

//		SimpleAdapter mAdapter=new SimpleAdapter(this, (List<? extends Map<String, ?>>) list,R.layout.item,new String[]{"name","key"},new int[]{R.id.name,R.id.key});
//		lt.setAdapter(mAdapter);
//		lt.setOnItemClickListener(new OnItemClickListener(){
//
//				@SuppressLint("WrongConstant")
//				@TargetApi(Build.VERSION_CODES.HONEYCOMB)
//				@Override
//				public void onItemClick(AdapterView<?> p1, View p2, int p3, long p4)
//				{
//					ClipboardManager clip=(ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
//					String key=(String)list.get(p3).get("truekey");
//					clip.setText(key);
//					Toast.makeText(MainActivity.this,"密码已复制",0).show();
//				}
//			});

		SimpleAdapter mAdapter=new SimpleAdapter(MainActivity.this, list,R.layout.item,new String[]{"name","key"},new int[]{R.id.name,R.id.key});
		lt.setAdapter(mAdapter);
		lt.setOnItemClickListener(new OnItemClickListener(){

				@SuppressLint("WrongConstant")
				@TargetApi(Build.VERSION_CODES.HONEYCOMB)
				@Override
				public void onItemClick(AdapterView<?> p1, View p2, int p3, long p4)
				{
					ClipboardManager clip=(ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
					String key=(String)list.get(p3).get("truekey");
					clip.setText(key);
					Toast.makeText(MainActivity.this,"密码已复制",0).show();
				}
			});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		
		menu.add(0,0,0,"关于");
	
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		
		if(item.getItemId()==0){
			new AlertDialog.Builder(this)
			.setTitle("关于")
			.setMessage("本软件用于查看wifi密码，需要root权限\n@ylzh")
			.setPositiveButton("确定",null)
			.show();
		}
		return super.onOptionsItemSelected(item);
	}
	
	
}
