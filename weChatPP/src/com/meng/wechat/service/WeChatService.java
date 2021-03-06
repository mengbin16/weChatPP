package com.meng.wechat.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import net.sf.json.JsonConfig;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.meng.wechat.entity.AccessToken;
import com.meng.wechat.entity.PictureText;
import com.meng.wechat.entity.TextMessage;
import com.meng.wechat.entity.WeChatUser;
import com.meng.wechat.util.WeChatUtils;

/**
 * 处理微信公众平台消息的服务类
 * @author meng
 */
public class WeChatService {
	
	/** 运行时日志 */
	private static final Log log = LogFactory.getLog(WeChatService.class);
	
	/** 微信接口调用凭证，每次调用前都会自动判断有效性。 */
	private AccessToken accessToken = null;
	
	/**
	 * 获取access_token
	 * @return access_token
	 */
	public AccessToken getAccessToken() {
		if (null == accessToken || !isTokenValid(accessToken)) {
			// 重新获取凭证
			accessToken = requestAccessToken();
		}
		return accessToken;
	}
	
	/**
	 * 判断access_token是否有效（未过期）。
	 * @param accessToken 微信接口调用凭证
	 * @return true：有效；false：无效
	 */
	private boolean isTokenValid (AccessToken accessToken) {
		int expiresIn = accessToken.getExpiresIn();
		Long time = accessToken.getTime();
		Long now = System.currentTimeMillis();
		if (now - time > expiresIn * 1000 - 10000) {
			// 为了保险起见，提前10秒钟即认为凭证过期了。
			return false;
		}
		return true;
	}
	
	/**
	 * 向微信服务器请求获取access_token
	 * @return access_token
	 */
	private AccessToken requestAccessToken (){
		Long time = System.currentTimeMillis();
	    String requestUrl = WeChatUtils.GET_ACCESSTOKEN_URL.replace("APPID", WeChatUtils.APPID).replace("APPSECRET", WeChatUtils.APPSECRET);
	    JSONObject value = WeChatUtils.httpsRequest(requestUrl, "GET", null);
	    String access_token = value.getString("access_token");
	    int expires_in = -1;
	    expires_in = value.getInt("expires_in");
	    if (null == access_token || -1 == expires_in){
	        int errcode = value.getInt("errcode");
	        String errmsg = value.getString("errmsg");
	        log.error("The error code is " + errcode + ". The error message is " + errmsg);
	        return null;
	    }
	    AccessToken accessToken = new AccessToken(access_token, expires_in, time);
	    return accessToken;
	}
	
	/**
	 * 处理微信发来的请求
	 * 
	 * @param request
	 * @return xml
	 */
	public static String processRequest(HttpServletRequest request) {
		// xml格式的消息数据
		String respXml = null;
		// 默认返回的文本消息内容
		String respContent = "我收到了。";
		try {
			// 调用parseXml方法解析请求消息
			Map<String, String> requestMap = WeChatUtils.parseXml(request);
			// 发送方帐号
			String fromUserName = requestMap.get("FromUserName");
			// 开发者微信号
			String toUserName = requestMap.get("ToUserName");
			log.info("发送者openId是：" + fromUserName + "，接收者openId是：" + toUserName);
			// 消息类型
			String msgType = requestMap.get("MsgType");
			// 消息创建时间
			String createTime = requestMap.get("CreateTime");
			
			// 文本消息
			if (msgType.equals(WeChatUtils.MESSAGE_TYPE_TEXT)) {
				String content = requestMap.get("Content");
//				respContent = ChatService.chat(fromUserName, createTime, content);
				respContent = content;
			}
			// 回复文本消息
			TextMessage textMessage = new TextMessage();
			textMessage.setToUserName(fromUserName);
			textMessage.setFromUserName(toUserName);
			textMessage.setCreateTime(System.currentTimeMillis());
			textMessage.setMsgType(WeChatUtils.MESSAGE_TYPE_TEXT);
			textMessage.setContent(respContent);
			// 将文本消息对象转换成xml
			respXml = WeChatUtils.messageToXml(textMessage);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return respXml;
	}
	
	/**
	 * 客服接口发送文本消息
	 * @param toUserName 普通用户openid
	 * @param content 文本消息内容
	 * @return 结果
	 */
	public JSONObject sendTextMessage (String toUserName, String content) {
		JSONObject data = new JSONObject();
		data.put("touser", toUserName);
		data.put("msgtype", "text");
		JSONObject text = new JSONObject();
		text.put("content", content);
		data.put("text", text);
		AccessToken accessToken = getAccessToken();
		String requestUrl = WeChatUtils.POST_CUSTOMER_SEND_URL.replace("ACCESS_TOKEN", accessToken.getAccessToken());
		return WeChatUtils.httpsRequest(requestUrl, "POST", data.toString());
	}
	
	/**
	 * 客服接口发送图片消息
	 * @param toUserName 普通用户openid
	 * @param mediaId 发送的图片消息的媒体ID
	 * @return 结果
	 */
	public JSONObject sendImageMessage (String toUserName, String mediaId) {
		JSONObject data = new JSONObject();
		data.put("touser", toUserName);
		data.put("msgtype", "image");
		JSONObject image = new JSONObject();
		image.put("media_id", mediaId);
		data.put("image", image);
		AccessToken accessToken = getAccessToken();
		String requestUrl = WeChatUtils.POST_CUSTOMER_SEND_URL.replace("ACCESS_TOKEN", accessToken.getAccessToken());
		return WeChatUtils.httpsRequest(requestUrl, "POST", data.toString());
	}
	
	/**
	 * 客服接口发送语音消息
	 * @param toUserName 普通用户openid
	 * @param mediaId 发送的语音消息的媒体ID
	 * @return 结果
	 */
	public JSONObject sendVoiceMessage (String toUserName, String mediaId) {
		JSONObject data = new JSONObject();
		data.put("touser", toUserName);
		data.put("msgtype", "voice");
		JSONObject voice = new JSONObject();
		voice.put("media_id", mediaId);
		data.put("voice", voice);
		AccessToken accessToken = getAccessToken();
		String requestUrl = WeChatUtils.POST_CUSTOMER_SEND_URL.replace("ACCESS_TOKEN", accessToken.getAccessToken());
		return WeChatUtils.httpsRequest(requestUrl, "POST", data.toString());
	}
	
	/**
	 * 客服接口发送视频消息
	 * @param toUserName 普通用户openid
	 * @param mediaId 发送的视频消息的媒体ID
	 * @param thumb_media_id 缩略图的媒体ID
	 * @param title 视频消息的标题
	 * @param description 视频消息的描述
	 * @return 结果
	 */
	public JSONObject sendVideoMessage (String toUserName, String mediaId, String thumb_media_id, String title, String description) {
		JSONObject data = new JSONObject();
		data.put("touser", toUserName);
		data.put("msgtype", "video");
		JSONObject video = new JSONObject();
		video.put("media_id", mediaId);
		video.put("thumb_media_id", thumb_media_id);
		video.put("title", title);
		video.put("description", description);
		data.put("video", video);
		AccessToken accessToken = getAccessToken();
		String requestUrl = WeChatUtils.POST_CUSTOMER_SEND_URL.replace("ACCESS_TOKEN", accessToken.getAccessToken());
		return WeChatUtils.httpsRequest(requestUrl, "POST", data.toString());
	}
	
	/**
	 * 客服接口发送音乐消息
	 * @param toUserName 普通用户openid
	 * @param musicurl 音乐链接
	 * @param hqmusicurl 高品质音乐链接，wifi环境优先使用该链接播放音乐
	 * @param thumb_media_id 缩略图的媒体ID
	 * @param title 音乐消息的标题
	 * @param description 音乐消息的描述
	 * @return 结果
	 */
	public JSONObject sendMusicMessage (String toUserName, String musicurl, String hqmusicurl, String thumb_media_id, String title, String description) {
		JSONObject data = new JSONObject();
		data.put("touser", toUserName);
		data.put("msgtype", "music");
		JSONObject music = new JSONObject();
		music.put("musicurl", musicurl);
		music.put("hqmusicurl", hqmusicurl);
		music.put("thumb_media_id", thumb_media_id);
		music.put("title", title);
		music.put("description", description);
		data.put("music", music);
		AccessToken accessToken = getAccessToken();
		String requestUrl = WeChatUtils.POST_CUSTOMER_SEND_URL.replace("ACCESS_TOKEN", accessToken.getAccessToken());
		return WeChatUtils.httpsRequest(requestUrl, "POST", data.toString());
	}
	
	/**
	 * 客服接口发送图文消息（点击跳转到外链） 
	 * @param toUserName 普通用户openid
	 * @param articleList 图文消息内容实体列表，条数限制在8条以内
	 * @return 结果
	 */
	public JSONObject sendNewsMessage (String toUserName, List<PictureText> articleList) {
		JSONObject data = new JSONObject();
		data.put("touser", toUserName);
		data.put("msgtype", "news");
		JSONObject news = new JSONObject();
		JSONArray articles = new JSONArray();
		for (int i = 0; i < articleList.size(); i++) {
			if (i >= 8) {
				// 最多支持八条
				break;
			}
			PictureText pictureText = articleList.get(i);
			JSONObject article = new JSONObject();
			article.put("title", pictureText.getTitle());
			article.put("description", pictureText.getDescription());
			article.put("url", pictureText.getUrl());
			article.put("picurl", pictureText.getPicurl());
			articles.add(article);
		}
		news.put("articles", articles);
		data.put("news", news);
		
		AccessToken accessToken = getAccessToken();
		String requestUrl = WeChatUtils.POST_CUSTOMER_SEND_URL.replace("ACCESS_TOKEN", accessToken.getAccessToken());
		return WeChatUtils.httpsRequest(requestUrl, "POST", data.toString());
	}
	
	/**
	 * 客服接口发送图文消息（点击跳转到图文消息页面）
	 * @param toUserName 普通用户openid
	 * @param media_id 发送的图文消息（点击跳转到图文消息页）的媒体ID
	 * @return 结果
	 */
	public JSONObject sendMpnewsMessage (String toUserName, String media_id) {
		JSONObject data = new JSONObject();
		data.put("touser", toUserName);
		data.put("msgtype", "mpnews");
		JSONObject mpnews = new JSONObject();
		mpnews.put("media_id", media_id);
		data.put("mpnews", mpnews);
		
		AccessToken accessToken = getAccessToken();
		String requestUrl = WeChatUtils.POST_CUSTOMER_SEND_URL.replace("ACCESS_TOKEN", accessToken.getAccessToken());
		return WeChatUtils.httpsRequest(requestUrl, "POST", data.toString());
	}
	
	/**
	 * 获取全部粉丝详细信息
	 */
	@SuppressWarnings("unchecked")
	public void getFans() {
		List<String> list = new ArrayList<String>();
		
		// 首先，获取粉丝openid列表
		AccessToken accessToken = getAccessToken();
		if (null != accessToken) {
			String next_openid = "";
			int getCount = 0;
			String requestUrl = WeChatUtils.GET_USERLIST_URL2.replace("ACCESS_TOKEN", accessToken.getAccessToken());
			while (null != next_openid) {
				JSONObject value = WeChatUtils.httpsRequest(requestUrl, "GET", null);
				try {
					int total = value.getInt("total");
					int count = value.getInt("count");
					getCount += count;
					next_openid = value.getString("next_openid");
					JSONObject data = value.getJSONObject("data");
					list.addAll((List<String>) JSONArray.toCollection(data.getJSONArray("openid")));
					if (getCount >= total) {
						break;
					}
					requestUrl = WeChatUtils.GET_USERLIST_URL.replace("ACCESS_TOKEN", accessToken.getAccessToken()).replace("NEXT_OPENID", next_openid);
				} catch (JSONException e) {
					int errcode = value.getInt("errcode");
			        String errmsg = value.getString("errmsg");
			        log.error("The error code is " + errcode + ". The error message is " + errmsg);
			        return;
				}
			}
			
		}
		
		// 其次，获取粉丝详细信息
		int size = list.size();
		if (size > 0) {
			// 批量获取粉丝详细信息
			JSONObject data = new JSONObject();
			JSONArray userList = new JSONArray();
			for (int i = 0; i < size; i++) {
				JSONObject user = new JSONObject();
				user.put("openid", list.get(i));
				user.put("lang", "zh-CN");
				userList.add(user);
				if ((i + 1) % 100 == 0) {
					// 100 的倍数，则发送一次请求获取用户详细信息
					data.put("user_list", userList);
					requestUserInfoBatch(data.toString());
					user = new JSONObject();
					userList = new JSONArray();
				}
			}
			if (userList.size() > 0) {
				data.put("user_list", userList);
				requestUserInfoBatch(data.toString());
			}
			
		}
		
	}
	
	/**
	 * 向微信服务器请求批量获取用户详细信息
	 * @param body 请求体
	 * @return 用户列表
	 */
	@SuppressWarnings("unchecked")
	private List<WeChatUser> requestUserInfoBatch(String body) {
		accessToken = getAccessToken();
		if (null == accessToken) {
			return null;
		}
		String requestUrl = WeChatUtils.POST_USERINFO_BATCH_URL.replace("ACCESS_TOKEN", accessToken.getAccessToken());
		JSONObject value = WeChatUtils.httpsRequest(requestUrl, "POST", body);
		if (null == value) {
			return null;
		}
		JSONArray userInfoList;
		try {
			userInfoList = value.getJSONArray("user_info_list");
		} catch (JSONException e) {
			int errcode = value.getInt("errcode");
	        String errmsg = value.getString("errmsg");
	        log.error("The error code is " + errcode + ". The error message is " + errmsg);
	        return null;
		}
		
		List<WeChatUser> weChatUserList = JSONArray.toList(userInfoList, new WeChatUser(), new JsonConfig());
		for (WeChatUser weChatUser : weChatUserList) {
			int subscribe = weChatUser.getSubscribe();
			String openid = weChatUser.getOpenid();
			if (subscribe == 0) {
				log.info("用户" + openid + "已取消关注。");
			} else {
				log.info("用户" + openid + "正在关注。");
			}
		}
		return weChatUserList;
	}

}
