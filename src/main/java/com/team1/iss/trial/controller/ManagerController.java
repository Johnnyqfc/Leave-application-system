package com.team1.iss.trial.controller;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.opencsv.CSVWriter;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.team1.iss.trial.common.CommConstants;
import com.team1.iss.trial.domain.LA;
import com.team1.iss.trial.domain.OverTime;
import com.team1.iss.trial.domain.User;
import com.team1.iss.trial.services.impl.LaServiceImpl;
import com.team1.iss.trial.services.impl.ManagerServiceImpl;
import com.team1.iss.trial.services.impl.OverTimeServiceImpl;
import com.team1.iss.trial.services.interfaces.ILaService;
import com.team1.iss.trial.services.interfaces.IManagerService;
import com.team1.iss.trial.services.interfaces.IOverTimeService;

@Controller
@RequestMapping("/manager")
public class ManagerController {
	
	@Autowired
	private IManagerService mservice;
	
	@Autowired
	public void setMservice(ManagerServiceImpl mServiceImpl) {
		this.mservice = mServiceImpl;
	}
	@Autowired
	private ILaService laservice;
	
	@Autowired
	public void setLaservice(LaServiceImpl laserviceimpl) {
		this.laservice = laserviceimpl;
	}
	
	@Autowired
	private IOverTimeService otservice;
	
	@Autowired
	public void setOtservice(OverTimeServiceImpl otserviceimpl) {
		this.otservice = otserviceimpl;
	}
	
	
	@RequestMapping("")
	public String managerHome() {
		return ("manager/mHome");
	}
	
	@RequestMapping("/listforapproval")
	public String listforapproval(Model model) {
		model.addAttribute("listforapproval",mservice.findPendingApplications());
		return "/manager/listforapproval";	
	}
	
	//show individual application
	@RequestMapping("/individual/{uid}")
	public String individualapplication(@PathVariable("uid") Integer uid,Model model ) {
		model.addAttribute("la", mservice.findLAByID(uid));
		return "/manager/individualapplication";
	}

	
	//approve leave, in detailed leave application html after clicking into certain application
	@RequestMapping("/approveapplication/{uid}")
	public String approveApplication(@PathVariable("uid") int uid, Model model) {
		LA la=laservice.getLaById(uid);
		la.setStatus(CommConstants.ApplicationStatus.APPROVED);
		mservice.saveLA(la);
		model.addAttribute("la", la);
		return "/manager/confirmation";
	}
	

	//reject leave 
	@RequestMapping(value="/rejectapplication",method = RequestMethod.GET)
	public String rejectApplication(String rejectreason,HttpSession session,Model model) {
		LA la=(LA)session.getAttribute("la");
		la.setStatus(CommConstants.ApplicationStatus.REJECTED);
		la.setRejectReason(rejectreason);
		laservice.saveLA(la);
		model.addAttribute("la", la);
		session.removeAttribute("la");
		return "/manager/confirmation";
	}
	
	//key in reject reason
	@RequestMapping("/rejectreasonkeyin/{uid}")
	public String keyInRejectReason(@PathVariable("uid") int uid,Model model,HttpSession session) {
		LA la=laservice.getLaById(uid);
		model.addAttribute("rejectreason", la.getRejectReason());
		session.setAttribute("la", la);
		return "/manager/rejectreason";
	}
	
	//Compensation Claims
	@RequestMapping("/compensationclaims")
	public String compensationclaims(Model model) {
		model.addAttribute("compensationclaims",mservice.findClaims());
		return "/manager/compensationclaims";	
	}
	
	//Approve Claim
	@RequestMapping("/approveClaim/{uid}")
	public String approveClaim(@PathVariable("uid") int uid, Model model) {
		OverTime ot=otservice.getOtById(uid);
		ot.setStatus(CommConstants.ClaimStatus.APPROVED);
		mservice.saveOverTime(ot);
		model.addAttribute("ot", ot);
		return "/manager/claimConfirmation";
	}
		
	//Reject Claim
	@RequestMapping("/rejectClaim/{uid}")
	public String rejectClaim(@PathVariable("uid") int uid, Model model) {
		OverTime ot=otservice.getOtById(uid);
		ot.setStatus(CommConstants.ClaimStatus.REJECTED);
		mservice.saveOverTime(ot);
		model.addAttribute("ot", ot);
		return "/manager/claimConfirmation";
	}
	
	//Employee Leave History
	@RequestMapping("/las/{uid}")
	public String la(@PathVariable("uid") Integer uid, Model model) {
		List<LA> las = laservice.findLaByOwnerId(uid);
		model.addAttribute("lalist", las);
		return "/manager/lalist";
	}
	
	@RequestMapping("/list")
	public String list() {
		return "forward:/manager/list/1";
	}	
	@RequestMapping("/list/{page}")
	public String listByPagination(@PathVariable("page") int page, Model model) {
		PageRequest pageable = PageRequest.of(page-1, 3);
		Page<User> userpage = mservice.getPaginatedEmployees(pageable);
        int totalPages = userpage.getTotalPages();
        if(totalPages > 0) {
            List<Integer> pageNumbers = IntStream.rangeClosed(1,totalPages).boxed().collect(Collectors.toList());
            model.addAttribute("pageNumbers", pageNumbers);
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        model.addAttribute("currentuseremail", auth.getName());
        model.addAttribute("userList", userpage.getContent());
		return "manager/subordinates";
	}
	
	@RequestMapping(value = "search", method = RequestMethod.GET)
	public String getUser(@RequestParam (value = "word", required = false) String word, Model model) {
		if (word.isEmpty()) {
			return "forward:/manager/list";
		}
		List<User> users= mservice.getAllEmployees(word); //check to name and email
	    model.addAttribute("users", users);
	    return "forward:/manager/list";
	}
	
	//retrieve employees under logged-in manager 
	@RequestMapping("/employeelist")
	public String getEmployeeListUnderManager(Model model) {
		
		int managerid=mservice.getCurrentUid();
		//find employee under this manager
		ArrayList<User> employeeList= mservice.getEmolyeeList(managerid);
		model.addAttribute("employeelist", employeeList);
		return "/manager/employeelistundermanager";
	}
	
	// export to csv
	@GetMapping("/exportcompensation")
    public void exportCSV(HttpServletResponse response) throws Exception {

        //set file name and content type
        String filename = "Compensation.csv";

        response.setContentType("text/csv");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + filename + "\"");

        //create a csv writer
        StatefulBeanToCsv<OverTime> writer = new StatefulBeanToCsvBuilder<OverTime>(response.getWriter())
                .withQuotechar(CSVWriter.NO_QUOTE_CHARACTER)
                .withSeparator(CSVWriter.DEFAULT_SEPARATOR)
                .withOrderedResults(false)
                .build();
        
        //write all claims to csv file
        ArrayList<OverTime> compensationlist=mservice.findClaims();
        writer.write(compensationlist);
	}

	//converte time format from unixstamp (for csv starttime and endtime format)
	/*
	 * public static String Timeformating(long ds) { Instant i =
	 * Instant.ofEpochSecond(ds/1000); ZoneId sgZone = ZoneId.of("Asia/Singapore");
	 * ZonedDateTime sgdt = ZonedDateTime.ofInstant(i, sgZone); DateTimeFormatter df
	 * = DateTimeFormatter .ofPattern("dd/MM/yyyy HH:mm"); return sgdt.format(df); }
	 */
	
//	//Approve Claim
//	@RequestMapping("/approveclaim/{uid}")
//	public String approveClaim(@PathVariable("uid") int uid, Model model) {
//		OverTime ot=otservice.getOverTimeById(uid);
//		ot.setStatus(CommConstants.ClaimStatus.APPROVED);
//		mservice.saveOverTime(ot);
//		model.addAttribute("ot", ot);
//		return "/manager/compensationclaims";
//	}
//		
//	//Reject Claim
//	@RequestMapping("/rejectclaim/{uid}")
//	public String rejectClaim(@PathVariable("uid") int uid, Model model) {
//		LA la=laservice.getLaById(uid);
//		la.setStatus(CommConstants.ClaimStatus.REJECTED);
//		mservice.saveOverTime(ot);
//		model.addAttribute("ot", ot);
//		return "/manager/compensationclaims";
//	}


}






