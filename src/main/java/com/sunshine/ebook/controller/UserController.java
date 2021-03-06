package com.sunshine.ebook.controller;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.*;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.sunshine.ebook.common.response.ContentResponse;
import com.sunshine.ebook.entity.Userinfo;
import com.sunshine.ebook.request.UserRequest;
import com.sunshine.ebook.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import java.util.Date;

@Api(tags = "用户相关接口")
@RestController
@RequestMapping(value = "/api/v1/user")
public class UserController {

	private static final Logger logger = LoggerFactory.getLogger(UserController.class);
	
	@Autowired
	private UserService userService;
	
	@ApiOperation(value = "判断手机号或邮箱是否存在")
	@RequestMapping(value = "/checkPhoneOrEmailIsRegist", method = RequestMethod.GET)
	public ResponseEntity<ContentResponse> checkEmailIsRegist(
			@ApiParam(value = "调用类型，0=注册，1=密码找回", required = true) @RequestParam("invokeType") int invokeType,
			@ApiParam(value = "注册类型，0=手机号注册，1=邮箱注册", required = true) @RequestParam("type") int type,
			@ApiParam(value = "手机号或邮箱", required = true) @RequestParam("target") String target) {
		String key = null;
		Userinfo condition = new Userinfo();
		if (0 == type) {
			key = "手机号";
			condition.setPhonenum(target);
		} else {
			key = "邮箱";
			condition.setEmail(target);
		}
		condition.setUserflag(0);
		Userinfo userinfo = userService.getUserinfoByCondition(condition);
		if (userinfo != null) {
			if (0 == invokeType) {
				ContentResponse response = new ContentResponse(HttpStatus.BAD_REQUEST.value(), key + "已被注册");
				return ResponseEntity.badRequest().body(response);
			} else {
				return ResponseEntity.ok().build();
			}
		} else {
			if (0 == invokeType) {
				return ResponseEntity.ok().build();
			} else {
				ContentResponse response = new ContentResponse(HttpStatus.BAD_REQUEST.value(), key + "不存在");
				return ResponseEntity.badRequest().body(response);
			}
		}
	}

	@ApiOperation(value = "发送验证码")
	@RequestMapping(value = "/sendCheckCode", method = RequestMethod.PUT)
	public ResponseEntity<ContentResponse> sendCheckCode(
			@ApiParam(value = "发送类型，0=注册，1=密码找回", required = true) @RequestParam("sendType") int sendType,
			@ApiParam(value = "注册类型，0=手机号，1=邮箱", required = true) @RequestParam("type") int type,
			@ApiParam(value = "手机号或邮箱", required = true) @RequestParam("target") String target) {
		boolean flag = userService.sendCheckCode(sendType, type, target);
		ContentResponse response = null;
		String value = "";
		if (1 == type) {
			value = "邮箱";
		} else {
			value = "手机号";
		}
		if (flag) {
			response = new ContentResponse(HttpStatus.OK.value(), "验证码已发送指定的" + value + "，请注意查收");
			return ResponseEntity.accepted().body(response);
		} else {
			response = new ContentResponse(HttpStatus.BAD_REQUEST.value(), "验证码发送失败");
			return ResponseEntity.badRequest().body(response);
		}
	}
	
	@ApiOperation(value = "用户注册")
	@RequestMapping(value = "/userRegister", method = RequestMethod.POST)
	public ResponseEntity<ContentResponse> userRegister(
			@ApiParam(value = "Json请求体", required = true) @RequestBody UserRequest userRequest) {
		boolean checkCodeIsValid = userService.checkCodeIsValid(userRequest);
		if (!checkCodeIsValid) {
			ContentResponse response = new ContentResponse(HttpStatus.BAD_REQUEST.value(), "验证码错误");
			return ResponseEntity.badRequest().body(response);
		}
		Userinfo userinfo = userService.checkCodeIsOverdue(userRequest);
		if (null == userinfo) {
			ContentResponse response = new ContentResponse(HttpStatus.BAD_REQUEST.value(), "验证码不在有效期");
			return ResponseEntity.badRequest().body(response);
		}
		Userinfo user = new Userinfo(userRequest);
		user.setUserid(userinfo.getUserid());
		user.setUserflag(0);
		boolean flag = userService.registerUserinfo(user);
		if (flag) {
			return ResponseEntity.ok().build();
		} else {
			ContentResponse response = new ContentResponse(HttpStatus.BAD_REQUEST.value(), "用户注册失败");
			return ResponseEntity.badRequest().body(response);
		}
	}
	
	@ApiOperation(value = "修改密码")
	@RequestMapping(value = "/modifyPassword", method = RequestMethod.POST)
	public ResponseEntity<ContentResponse> modifyPassword(
			@ApiParam(value = "Json请求体", required = true) @RequestBody UserRequest userRequest) {
		boolean checkCodeIsValid = userService.checkCodeIsValid(userRequest);
		if (!checkCodeIsValid) {
			ContentResponse response = new ContentResponse(HttpStatus.BAD_REQUEST.value(), "验证码错误");
			return ResponseEntity.badRequest().body(response);
		}
		Userinfo userinfo = userService.checkCodeIsOverdue(userRequest);
		if (null == userinfo) {
			ContentResponse response = new ContentResponse(HttpStatus.BAD_REQUEST.value(), "验证码不在有效期");
			return ResponseEntity.badRequest().body(response);
		}
		Userinfo user = new Userinfo(userRequest);
		user.setUserid(userinfo.getUserid());
		boolean flag = userService.updateUserinfo(user);
		if (flag) {
			return ResponseEntity.ok().build();
		} else {
			ContentResponse response = new ContentResponse(HttpStatus.BAD_REQUEST.value(), "修改密码失败");
			return ResponseEntity.badRequest().body(response);
		}
	}

	@ApiOperation(value = "用户登录")
	@RequestMapping(value = "/userLogin", method = RequestMethod.POST)
	public ResponseEntity<Object> userLogin(
			@ApiParam(value = "手机号或邮箱", required = true) @RequestParam("target") String target,
			@ApiParam(value = "密码", required = true) @RequestParam("password") String password) {
		String username = target;
		UsernamePasswordToken token = new UsernamePasswordToken(target, password);
		//获取当前的Subject
		Subject subject = SecurityUtils.getSubject();
		try {
			//在调用了login方法后,SecurityManager会收到AuthenticationToken,并将其发送给已配置的Realm执行必须的认证检查
			//每个Realm都能在必要时对提交的AuthenticationTokens作出反应
			//所以这一步在调用login(token)方法时,它会走到MyRealm.doGetAuthenticationInfo()方法中,具体验证方式详见此方法
			logger.info("对用户[" + username + "]进行登录验证..验证开始");
			subject.login(token);
			logger.info("对用户[" + username + "]进行登录验证..验证通过");
			Userinfo userinfo = new Userinfo();
			userinfo.setLastlogtime(new Date());
			if (target.indexOf("@") != -1) {
				userinfo.setEmail(target);
			} else {
				userinfo.setPhonenum(target);
			}
			Userinfo user = userService.getUserinfoByCondition(userinfo);
			userinfo.setUserid(user.getUserid());
			userService.updateUserinfo(userinfo);
			return ResponseEntity.ok().body(user);
		}catch(UnknownAccountException uae){
			logger.info("对用户[" + username + "]进行登录验证..验证未通过,未知账户");
			ContentResponse response = new ContentResponse(HttpStatus.BAD_REQUEST.value(), "未知账户");
			return ResponseEntity.badRequest().body(response);
		}catch(IncorrectCredentialsException ice){
			logger.info("对用户[" + username + "]进行登录验证..验证未通过,错误的凭证");
			ContentResponse response = new ContentResponse(HttpStatus.BAD_REQUEST.value(), "密码不正确");
			return ResponseEntity.badRequest().body(response);
		}catch(LockedAccountException lae){
			logger.info("对用户[" + username + "]进行登录验证..验证未通过,账户已锁定");
			ContentResponse response = new ContentResponse(HttpStatus.BAD_REQUEST.value(), "账户已锁定");
			return ResponseEntity.badRequest().body(response);
		}catch(ExcessiveAttemptsException eae){
			logger.info("对用户[" + username + "]进行登录验证..验证未通过,错误次数过多");
			ContentResponse response = new ContentResponse(HttpStatus.BAD_REQUEST.value(), "用户名或密码错误次数过多");
			return ResponseEntity.badRequest().body(response);
		}catch(AuthenticationException ae){
			logger.info("对用户[" + username + "]进行登录验证..验证未通过,堆栈轨迹如下");
			ContentResponse response = new ContentResponse(HttpStatus.BAD_REQUEST.value(), "账户或密码不正确");
			return ResponseEntity.badRequest().body(response);
		}
	}

	@ApiOperation(value = "用户退出登录")
	@RequestMapping(value = "/userLogout", method = RequestMethod.POST)
	public ResponseEntity<ContentResponse> userLogout() {
		//获取当前的Subject
		Subject subject = SecurityUtils.getSubject();
		String username = String.valueOf(subject.getPrincipal());
		try {
			//在调用了login方法后,SecurityManager会收到AuthenticationToken,并将其发送给已配置的Realm执行必须的认证检查
			//每个Realm都能在必要时对提交的AuthenticationTokens作出反应
			//所以这一步在调用login(token)方法时,它会走到MyRealm.doGetAuthenticationInfo()方法中,具体验证方式详见此方法
			subject.logout();
			logger.info("用户[" + username + "]退出登录");
			return ResponseEntity.ok().build();
		}catch(Exception e){
			logger.info("对用户[" + username + "]退出登录失败");
			ContentResponse response = new ContentResponse(HttpStatus.BAD_REQUEST.value(), "退出登录失败");
			return ResponseEntity.badRequest().body(response);
		}
	}

}
