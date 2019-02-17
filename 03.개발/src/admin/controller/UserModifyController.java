package admin.controller;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.sql.SQLException;

import javax.swing.JOptionPane;

import admin.dao.AdminDAO;
import admin.util.AdminUtil;
import admin.view.AdminMgMtView;
import admin.view.SearchAddrView;
import admin.view.UserModifyView;
import admin.vo.UserInfoVO;
import admin.vo.UserModifyVO;

public class UserModifyController extends WindowAdapter implements ActionListener {

	private UserModifyView umv;
	private AdminMgMtView ammv;
	private AdminMgMtController ammc;
	private String addrSeq;
	private AdminUtil au;
	
	public UserModifyController(UserModifyView umv, AdminMgMtView ammv, String addrSeq, AdminMgMtController ammc) {
		this.umv = umv;
		this.ammv = ammv;
		this.addrSeq = addrSeq;
		this.ammc = ammc;
		au = new AdminUtil();
	}
	
	public void msgCenter(String msg) {
		JOptionPane.showMessageDialog(umv, msg);
	}
	
	public void remove() {
		
		String id = umv.getJtfId().getText().trim();
		
		switch(JOptionPane.showConfirmDialog(umv, "회원정보로 등록된 기록도 모두 삭제됩니다.\n정말 삭제하시겠습니까?")) {
		case JOptionPane.OK_OPTION:
			try {
				AdminDAO.getInstance().deleteUser(id);
				umv.dispose();
				msgCenter("회원정보가 삭제되었습니다.");
				au.sendLog(umv.getJtfId().getText()+" 회원 정보 삭제");
				ammc.setUser();
			} catch (SQLException e) {
				msgCenter("DB에 문제가 발생했습니다.");
				e.printStackTrace();
			}
			break;
		case JOptionPane.NO_OPTION:
		case JOptionPane.CANCEL_OPTION:
		}
	}
	
	public boolean checkPass(String pass) { // 비밀번호 검증, 최대 12자리, 대문자 소문자 특수문자 조합
		boolean resultFlag = false;
		
		boolean lowerCaseFlag = false;
		boolean upperCaseFlag = false;
		boolean spSymbolFlag = false;
		
		char[] lowerCase = { 
				'a','b','c','d','e','f','g',
				'h','i','j','k','l','m','n','o','p','q','r',
				's','t','u','v','w','x','y','z'};
		
		char[] upperCase = {
				'A','B','C','D','E','F','G',
				'H','I','J','K','L','M','N','O','P','Q','R',
				'S','T','U','V','W','X','Y','Z'};
		
		char[] spSymbol = {'!','@','#','$','%','^','&','*','(',')','-','_','+','='};
		
		if(!(pass.equals("") || pass.length() > 13)) {

			for(int i=0; i<pass.length(); i++) {
				for(int j=0; j<lowerCase.length; j++) {
					if(pass.charAt(i) == lowerCase[j]) {
						lowerCaseFlag = true;
					}
				}
				for(int j=0; j<upperCase.length; j++) {
					if(pass.charAt(i) == upperCase[j]) {
						upperCaseFlag = true;
					}
				}
				for(int j=0; j<spSymbol.length; j++) {
					if(pass.charAt(i) == spSymbol[j]) {
						spSymbolFlag = true;
					}
				}
			}
			
			if(lowerCaseFlag && upperCaseFlag && spSymbolFlag) {
				resultFlag = true;
			}
		}
		return resultFlag;
	}
	
	public boolean checkSsn(String input) {
		boolean flag = false;
		
		String ssn = input.replaceAll("-", "");
		
		if (ssn.length() != 13) { // 13자리('-'제외)
			flag = false;
			return flag;
		}
		
		int[] validVal = new int[12];
		int sumOfValidVal = 0;
		int j = 2;
		
		for(int i=0; i<12; i++) {
			
			if(j>9) {
				j = 2;
			}
			
			validVal[i] = Character.getNumericValue(ssn.charAt(i))*j;
			j++;
			
			sumOfValidVal += validVal[i];
		}
		
		if ((11 - (sumOfValidVal%11))%10 == Character.getNumericValue(ssn.charAt(12))) {
			flag = true;
		}
		/*-- '880101-1234567'
		-- 각 자리에 지정한 수를 곱함
		--  234567 892345
		-- 마지막 주민번호 한자리가 검증 값
		-- 각 자리별 결과를 다 더한 후 11로 나눈 나머지를 구함
		-- 그 결과를 11에서 뺀다
		-- 그 결과를 10으로 나눈 나머지를 구함
		-- 최종 결과값이 주민번호 최종끝자리와 같으면 유효
		-- 같지 않으면 무효*/
		
		return flag;
	}
	
	public boolean checkTel(String input) {
		boolean flag = false;
		
		String tel = input.replaceAll("-", ""); 
		
		try {
			Integer.parseInt(tel); // 입력값이 정수인지 판단
		} catch (NumberFormatException npe) {
			return flag;
		}
		
		if(tel.length() != 11) { // 000-0000-0000 11자리 수가 아니라면
			return flag;
		} else {
			flag = true;
		}
		
		return flag;
	}
	
	public boolean checkEmail(String email) { 
		boolean flag = false;
		
		if (email.length() < 13) { // 이메일은 @,.포함 최소 14자 이상이어야 함
			flag = false;
			return flag;
		}
		
		if (email.indexOf("@") != -1 && email.indexOf(".") != -1) { // @와 .이 있다면
			flag = true;
		}
		
		return flag;
	}
	
	public void modify() {
		UserModifyVO umvo = null;
		
		String id = umv.getJtfId().getText().trim();
		String pass = new String(umv.getJpfPass().getPassword()).trim();
		
		if(!checkPass(pass)) { // 비밀번호 검증
			msgCenter("비밀번호를 확인해주세요.\n 최대 12자리, 대소문자, 특수문자 조합으로 만들어주세요.");
			return;
		}
		
		String name = umv.getJtfName().getText().trim();
		
		String ssn = umv.getJtfSsn1().getText().trim()+"-"+umv.getJtfSsn2().getText().trim();
		
		if(!checkSsn(ssn)) { // ssn 검증
			msgCenter("올바른 주민번호가 아닙니다.\n다시 입력해주세요.");
			return;
		}
		
		String tel = umv.getJtfTel().getText().trim();
		
		if(!checkTel(tel)) { // tel 검증
			msgCenter("올바른 연락처가 아닙니다.\n예)010-0000-0000\n다시 입력해주세요.");
			return;
		}
		String addrDetail = umv.getJtfAddr2().getText().trim();
		
		// email - @ . 필수, 14자리 이상(@.포함)
		String email = umv.getJtfEmail().getText().trim();
		
		if(!checkEmail(email)) { // email 검증
			msgCenter("올바른 이메일이 아닙니다. \n예)someid@domain.com\n다시 입력해주세요.");
			return;
		}
		
		String questionType = String.valueOf(umv.getJcbQuestion().getSelectedIndex());
		String answer = umv.getJtfAnswer().getText().trim();
		String userType = umv.getJcbUser().getSelectedItem().equals("일반") ? "E" : "R";
		
		umvo = new UserModifyVO(id, pass, name, ssn, tel, addrSeq, addrDetail, email, questionType, answer, userType);
		
		try {
			if(AdminDAO.getInstance().updateUser(umvo)) {
				msgCenter("회원정보가 수정되었습니다.");
				au.sendLog(umv.getJtfId().getText()+" 회원 정보 수정");
				umv.dispose();
				UserInfoVO ulvo = AdminDAO.getInstance().selectOneUser(id);
				ammc.setUser();
				new UserModifyView(ammv, ulvo, ammc);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public String searchAddr() {
		String addr1 = "";
		new SearchAddrView(umv, this);
		
		return addr1;
	}
	
	
	
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == umv.getJbModify()) {
			modify();
		}
		
		if (e.getSource() == umv.getJbRemove()) {
			remove();
		}
		
		if (e.getSource() == umv.getJbSearchAddr()) {
			searchAddr();
		}
		
		if(e.getSource() == umv.getJbClose()) {
			umv.dispose();
		}
	}
	
	@Override
	public void windowClosing(WindowEvent e) {
		umv.dispose();
	}

	public UserModifyView getUmv() {
		return umv;
	}

	public void setAddrSeq(String addrSeq) {
		this.addrSeq = addrSeq;
	}
	public String getAddrSeq() {
		return addrSeq;
	}
	
}
