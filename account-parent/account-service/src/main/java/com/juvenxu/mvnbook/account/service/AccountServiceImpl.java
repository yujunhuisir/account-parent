package com.juvenxu.mvnbook.account.service;

import java.util.HashMap;
import java.util.Map;

import com.juvenxu.mvnbook.account.captcha.AccountCaptchaException;
import com.juvenxu.mvnbook.account.captcha.AccountCaptchaService;
import com.juvenxu.mvnbook.account.captcha.RandomGenerator;
import com.juvenxu.mvnbook.account.email.AccountEmailService;
import com.juvenxu.mvnbook.account.exception.AccountEmailException;
import com.juvenxu.mvnbook.account.persist.Account;
import com.juvenxu.mvnbook.account.persist.AccountPersistException;
import com.juvenxu.mvnbook.account.persist.AccountPersistService;

public class AccountServiceImpl implements AccountService {
	
	
	private AccountPersistService accountPersistService;
	private AccountEmailService accountEmailService;
	private AccountCaptchaService accountCaptchaService;
	private Map<String,String>activationMap = new HashMap<String,String>();
	
	
	
	public AccountPersistService getAccountPersistService() {
		return accountPersistService;
	}

	public void setAccountPersistService(AccountPersistService accountPersistService) {
		this.accountPersistService = accountPersistService;
	}

	public AccountEmailService getAccountEmailService() {
		return accountEmailService;
	}

	public void setAccountEmailService(AccountEmailService accountEmailService) {
		this.accountEmailService = accountEmailService;
	}

	public AccountCaptchaService getAccountCaptchaService() {
		return accountCaptchaService;
	}

	public void setAccountCaptchaService(AccountCaptchaService accountCaptchaService) {
		this.accountCaptchaService = accountCaptchaService;
	}
	
	
	

	public String generateCaptchaKey() throws AccountServiceException {
		try {
			return accountCaptchaService.generateCaptchaKey();
		}catch(AccountCaptchaException e) {
			throw new AccountServiceException("Unable to generate Captche key.",e);
		}
		
	}

	public byte[] generateCaptchaImage(String captchaKey) throws AccountServiceException {
		try {
			return accountCaptchaService.generateCaptchaImage(captchaKey);
		}catch(AccountCaptchaException e) {
			throw new AccountServiceException("Unable to generate Captche Image.",e);
		}
	}

	public void signUp(SignUpRequest signUpRequest) throws AccountServiceException {
		try {
			if(!signUpRequest.getPassword().equals(signUpRequest.getConfirmPassword())) {
				throw new AccountServiceException("2 passwords do not match.");
			}
			if(!accountCaptchaService.validateCaptcha(signUpRequest.getCaptchaKey(), signUpRequest.getCaptchaValue())) {
				throw new AccountServiceException("incorrect captcha.");
			}
			
			Account account = new Account();
			account.setId(signUpRequest.getId());
			account.setEmail(signUpRequest.getEmail());
			account.setPassword(signUpRequest.getPassword());
			account.setActivated(false);
			
			accountPersistService.createAccount(account);
			String activationId = RandomGenerator.getRandomString();
			activationMap.put(activationId, account.getId());
			String link = signUpRequest.getActivateServiceUrl().endsWith("/")?signUpRequest
					.getActivateServiceUrl()+activationId:signUpRequest.getActivateServiceUrl()+"?key"+activationId;
			accountEmailService.sendMail(account.getEmail(), "Please Activate Your Account", link);
			
			
		}catch(AccountCaptchaException e) {
			throw new AccountServiceException("Ubable to validate captcha."+e);
		} catch (AccountPersistException e) {
			throw new AccountServiceException("Ubable to create account."+e);
		} catch (AccountEmailException e) {
			throw new AccountServiceException("Ubable to send actiavtion mail."+e);
		}
		

	}

	public void activivate(String activationId) throws AccountServiceException {
		String accountId = activationMap.get(activationId);
		if(accountId==null) {
			throw new AccountServiceException("Invalid account activation ID.");
		}
		try {
			Account account = accountPersistService.readAccount(accountId);
			account.setActivated(true);
			accountPersistService.updateAccount(account);
		}catch(AccountPersistException e) {
			throw new AccountServiceException("Unable to activate account");
		}

	}

	public void login(String id, String password) throws AccountServiceException {
		try {
			Account account = accountPersistService.readAccount(id);
			if(account==null) {
				throw new AccountServiceException("Account does not exist.");
			}
			if(!account.isActivated()) {
				throw new AccountServiceException("account is disable");
			}
			if(!account.getPassword().equals(password)) {
				throw new AccountServiceException("Incorrect password.");
			}
		}catch(AccountPersistException  e) {
			throw new AccountServiceException("Unable to log in.",e);
		}

	}

}
