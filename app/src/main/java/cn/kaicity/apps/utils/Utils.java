package cn.kaicity.apps.utils;

import java.io.*;

public class Utils
{
	
	public static String getPermission(String path)
	{
		
		Process process = null;
		DataOutputStream os = null;
		StringBuilder sb=new StringBuilder();
		try {
			String cmd = "cat "+path ;
			process = Runtime.getRuntime().exec("su"); // 切换到root帐号
			os = new DataOutputStream(process.getOutputStream());
			os.writeBytes(cmd + "\n");
			os.writeBytes("exit\n");
			os.flush();
			BufferedReader bf=new BufferedReader(new InputStreamReader(process.getInputStream()));
			String str="";
			while((str=bf.readLine())!=null){
				sb.append(str);
				sb.append("\n");
			}
			process.waitFor();
		} catch (Exception e) {
			return e.toString();
		} finally {

			try {
				if (os != null) {
					os.close();
				}
				process.destroy();
			} catch (Exception e) {
				e.printStackTrace();
				return e.toString();
			}
		}
		return sb.toString();
    }
}
