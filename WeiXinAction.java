package com.weixin.action;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.apache.struts2.ServletActionContext;
import org.apache.struts2.convention.annotation.Action;
import org.apache.struts2.convention.annotation.Namespace;
import org.apache.struts2.convention.annotation.ParentPackage;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;


@Controller("weixinAction")
@Scope("prototype")
@Namespace("/weixin")
@ParentPackage(value="struts-default")
public class WeiXinAction {
  Logger log = Logger.getLogger(this.getClass());

  private static String TOKEN = "你的app key";


	public HttpServletRequest getRequest() {
		return ServletActionContext.getRequest();
	}


	@Action(value="query")
	public String query(){
		PrintWriter pw = null;
		HttpServletResponse response = ServletActionContext.getResponse();
		response.setContentType("text/xml");
		response.setCharacterEncoding("utf-8");

		try{
			pw = ServletActionContext.getResponse().getWriter();
			//来源验证
			if(getRequest().getMethod().toLowerCase().equals("get")){
				log.info("weixin check Signature >>>");
				String echostr = Tools.getStringParameter(getRequest(), "echostr", "");
				if(checkSignature()){
					pw.print(echostr);
				}
			}
			//消息回复
			if(getRequest().getMethod().toLowerCase().equals("post")){
				log.info("weixin send message >>>");
				if(!checkSignature()){//签名错误
					return null;
				}
				//读取post xml
				String xml = readXML(getRequest().getInputStream());
				if(xml.equals(""))return null;
				//log.info(data);
				String[] message = buildMessage(xml);				
				String content = message[4];
				log.info("content:"+content);
        
				String reply = "我是回复";
        if(null==content){
        	reply = "只接受文本信息";
  			} 
				if(content.equals("Hello2BizUser")){
      		reply = "欢迎新用户";
  			} 


				String send = responseMessage(message[1], message[0], reply);

				pw.print(send);
			}
		}catch(Exception e){
			log.error(e.getMessage(),e);
		}
		return null;
	}

	public boolean checkSignature(){
		String signature = Tools.getStringParameter(getRequest(), "signature", "");
		String timestamp = Tools.getStringParameter(getRequest(), "timestamp", "");
		String nonce = Tools.getStringParameter(getRequest(), "nonce", "");
		//log.info("TOKEN:"+TOKEN+" signature:"+signature+" timestamp:"+timestamp+" nonce:"+nonce);
		String array[] = {TOKEN,timestamp,nonce};
		Arrays.sort(array);
		String str = "";
		for(int i=0;i<array.length;i++){
			str+=array[i];
		}
		//log.info("uncode:"+str);
		String encode = Sha1Util.sha1(str);
		//log.info("encode:"+encode+" signature:"+signature);
		if(encode.equals(signature)){
			return true;
		}
		return false;
	}

	public String readXML(ServletInputStream servletInputStream)throws Exception{
		BufferedReader br = new BufferedReader(new InputStreamReader(servletInputStream));  
        String line = null;  
        StringBuilder sb = new StringBuilder();  
        while((line = br.readLine())!=null){  
            sb.append(line);  
        }
        return sb.toString();
	}

	public String[] buildMessage(String message)throws Exception{
		String result[] = new String[5];
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		StringReader stringReader  =  new StringReader(message);
		Document document = builder.parse(new InputSource(stringReader));
		Element rootElement = document.getDocumentElement(); 
		NodeList bookNodes = rootElement.getChildNodes();
		for(int i=0;i<bookNodes.getLength();i++){
			Element element = (Element) bookNodes.item(i);  
			//log.info("getNodeName():"+element.getNodeName()+" getTextContent():"+element.getTextContent());
			if(element.getNodeName().equals("ToUserName")){
				result[0] = element.getTextContent();
			}
			if(element.getNodeName().equals("FromUserName")){
				result[1] = element.getTextContent();
			}
			if(element.getNodeName().equals("CreateTime")){
				result[2] = element.getTextContent();
			}
			if(element.getNodeName().equals("MsgType")){
				result[3] = element.getTextContent();
			}
			if(element.getNodeName().equals("Content")){
				result[4] = element.getTextContent();
			}
		}
		return result;
	}

	public String responseMessage(String toUserName,String fromUserName,String message){
		String response = "<xml><ToUserName><![CDATA[%s]]></ToUserName>"+
						 "<FromUserName><![CDATA[%s]]></FromUserName>"+
						 "<CreateTime>%s</CreateTime>"+
						 "<MsgType><![CDATA[text]]></MsgType>"+
						 "<Content><![CDATA[%s]]></Content>" +
						 "<FuncFlag>0</FuncFlag></xml>";
		return String.format(response, toUserName,fromUserName,String.valueOf(System.currentTimeMillis()),message);
	}

}
