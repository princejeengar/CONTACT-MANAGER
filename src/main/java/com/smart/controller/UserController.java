package com.smart.controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.smart.dao.ContactRepository;
import com.smart.dao.UserRepository;
import com.smart.entities.Contact;
import com.smart.entities.User;
import com.smart.helper.Message;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ContactRepository contactRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    @ModelAttribute
    public void addCommonData(Model model, Principal principal) {
        String userName = principal.getName();
        //System.out.println("USERNAME " + userName);

        User user = userRepository.getUserByUserName(userName);
        //System.out.println("USER " + user);

        model.addAttribute("user", user);
    }

    //user home
    @GetMapping("/index")
    public String dashboard(Model model) {
        model.addAttribute("title", "User Dashboard");
        return "normal/user_dashboard";
    }

    //contact handler
    @GetMapping("/addcontact")
    public String openAddContactForm(Model model) {
        model.addAttribute("title", "Add Contact");
        model.addAttribute("contact", new Contact());
        return "normal/add_contact_form";
    }

    //adding contact
    @PostMapping("/process-contact")
    public String processContact(@ModelAttribute Contact contact,
                                 @RequestParam("profileImage") MultipartFile file,
                                 Principal principal, HttpSession session, Model model) {
        try {
            String name = principal.getName();
            User user = this.userRepository.getUserByUserName(name);

            if (file.isEmpty()) {
                System.out.println("Image is Empty");
                contact.setImage("contact.png");
            } else {
                contact.setImage(file.getOriginalFilename());
                File savefile = new ClassPathResource("static/img").getFile();
                Path path = Paths.get(savefile.getAbsolutePath() + File.separator + file.getOriginalFilename());
                Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Image is uploaded");
            }

            user.getContacts().add(contact);
            contact.setUser(user);
            this.userRepository.save(user);
            System.out.println(contact);
            System.out.println("Successfully Added");
            session.setAttribute("message", new Message("Contact Added Successfully", "success"));

        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
            session.setAttribute("message", new Message("Something went wrong", "danger"));
        }

        return "redirect:/user/addcontact";
    }

	//contact list
    @GetMapping("/showContacts/{page}")
	public String showContacts(@PathVariable("page") Integer page, Model model, Principal principal) {

		// Here we are providing the page number and number of contact per page
		// Taking the page number as path variable form the user
		Pageable pageable = PageRequest.of(page, 5);

		model.addAttribute("title", "Show user Contacts");
		String name = principal.getName();
		User user = this.userRepository.getUserByUserName(name);
		Page<Contact> contacts = this.contactRepository.findContactByUser(user.getId(), pageable);
		model.addAttribute("contacts", contacts);
		model.addAttribute("currentPage", page);
		model.addAttribute("totalPage", contacts.getTotalPages());
		return "normal/showContacts";
	}
	
	//contact show handler
    @GetMapping("/{cId}/contact")
	public String showContactDetail(@PathVariable("cId") Integer cId, Model model, Principal principal) {
		//System.out.println("CID" +cId);
		
		Optional<Contact> contactOptional = this.contactRepository.findById(cId);
		Contact contact = contactOptional.get();
		
		String userName = principal.getName();
		User user = this.userRepository.getUserByUserName(userName);
		
		if(user.getId()==contact.getUser().getId()) {
			model.addAttribute("contact", contact);
			model.addAttribute("title", contact.getName());
		}
		
		
		return "normal/contact_detail";
	}
	
	//Delete handler
	@GetMapping("/delete/{cid}")
	public String deleteContact(@PathVariable("cid") Integer cId, HttpSession session) {

		// Deleting the image in the folder
		try {
			//Contact contact = 
			this.contactRepository.findById(cId).get();
			//String image = contact.getImage();
			//File saveFile = new ClassPathResource("static/img").getFile();
			//Path path = Paths.get(saveFile.getAbsolutePath() + "/" + image);
			//Files.deleteIfExists(path);

			this.contactRepository.deleteById(cId);
			session.setAttribute("message", new Message("Contact deleted succesfully...", "success"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "redirect:/user/showContacts/0";
	}
	
	//update contact handler
	@PostMapping("/update-contact/{cid}")
	public String updateForm(@PathVariable("cid") Integer cid, Model m) {
		
		m.addAttribute("title", "Update Contact");
		Contact contact = this.contactRepository.findById(cid).get();
		m.addAttribute("contact", contact);
		
		return "normal/update_form";
	}
	
	// Handler for the process the information of the updated contact

		@PostMapping("/process-update/{cId}")
		public String updateContact(@PathVariable("cId") Integer cId, @ModelAttribute Contact contact, Model model,
				@RequestParam("profileImage") MultipartFile file, Principal principal, HttpSession session) {
			System.out.println(file);

			try {
				if (file.isEmpty()) {
					Contact contact2 = this.contactRepository.findById(cId).get();
					String image = contact2.getImage();
					System.out.println(image);
					contact.setImage(image);
				} else {

					contact.setImage(file.getOriginalFilename());
					File saveFile = new ClassPathResource("static/img").getFile();
					Path path = Paths.get(saveFile.getAbsolutePath() + "/" + file.getOriginalFilename());
					Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
					contact.setImage(file.getOriginalFilename());

				}
				String name = principal.getName();
				User user = this.userRepository.getUserByUserName(name);
				contact.setUser(user);
				Contact updatedContact = this.contactRepository.save(contact);
				model.addAttribute("contact", updatedContact);
				session.setAttribute("message", new Message("Contact Updated succesfully...", "success"));

			} catch (Exception e) {
				e.printStackTrace();
			}
			return "normal/contact_detail";
		}
		
		// Process password handler
		@PostMapping("/process-password")
		public String changePassword(Principal principal,@RequestParam("oldPassword") String oldPassword,
				@RequestParam("newPassword") String newPassword, @RequestParam("reEnterPassword") String reEnterPassword,HttpSession session) {
			String name = principal.getName();
			User user = this.userRepository.getUserByUserName(name);
			String password = user.getPassword();
			//String oldEnPassword = 
					this.passwordEncoder.encode(oldPassword);
			//String newEnPasword = 
					this.passwordEncoder.encode(newPassword);
			//String reEnterEnPassword = 
					this.passwordEncoder.encode(reEnterPassword);
			
			//checking input from the user
			//System.out.println("password >>>>>>>>>>>>>>>>>>"+password);
			//System.out.println("old password  >>>>>>>>>>>>>>>>>>"+oldEnPassword);
			//System.out.println("new password  >>>>>>>>>>>>>>>>>>"+newEnPasword);
			//System.out.println("Re-enter password  >>>>>>>>>>>>>>>>>>"+reEnterEnPassword);
			
			boolean matches=false;
			matches = passwordEncoder.matches(newPassword, password);
			
			if(newPassword.isEmpty()) {
				
				session.setAttribute("message", new Message("Password can't be Empty", "danger"));
				 return "normal/user_setting";
			}
		
			
			if(passwordEncoder.matches(oldPassword, password)) {
				if(newPassword.equals(reEnterPassword)&& matches==false ) {
					
					user.setPassword(this.passwordEncoder.encode(newPassword));
					this.userRepository.save(user);
					session.setAttribute("message", new Message("Password changed successfully", "success"));
					
				}else if(matches==true){
					session.setAttribute("message", new Message("Old Password can't be a new password", "danger"));
					
				}
				
				else{
					session.setAttribute("message", new Message("New Password did not match!!!","danger"));
					return "normal/user_setting";
				}
			}else {
				session.setAttribute("message", new Message("Old Password did't match!!!", "danger"));
			}
			
			
			return "normal/user_setting";
		}
		
		//Delete user handler
		
		@GetMapping("/delete-user/{id}")
		public String deleteUser(@PathVariable("id")Integer id,HttpSession session) {
			
			this.userRepository.deleteById(id);
			session.setAttribute("message", new Message("User deleted successfully", "success"));
			
			return "/signup";
			
			
		}
	

		// Handler for updating the user information

		@PostMapping("/update-user/{id}")
		public String userProfile_update(@PathVariable("id") Integer id, Model model) {
			User user = this.userRepository.findById(id).get();
			model.addAttribute("user", user);
			model.addAttribute("title", "Update-User");
			return "normal/update_user";
		}
	
		// Handler for the process user update
		@PostMapping("/process-user-update")
		public String processUserUpdate(@ModelAttribute User user, Model model,
				@RequestParam("profileImage") MultipartFile file, HttpSession session) {

			int id = user.getId();
			try {
				if (file.isEmpty()) {
					User user2 = this.userRepository.findById(id).get();
					String image = user2.getImageUrl();
					user.setImageUrl(image);
				} else {

					user.setImageUrl(file.getOriginalFilename());
					File saveFile = new ClassPathResource("static/img").getFile();
					Path path = Paths.get(saveFile.getAbsolutePath() + "/" + file.getOriginalFilename());
					Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
					user.setImageUrl(file.getOriginalFilename());

				}
				User UpdatedUser = this.userRepository.save(user);
				model.addAttribute("user", UpdatedUser);
				session.setAttribute("message", new Message("Information updated successfully ", "alert-success"));
			} catch (Exception e) {
				e.printStackTrace();
			}

			return "normal/update_user";

		}
	
	
	
	
	
	//profile handler
	@GetMapping("/profile")
	public String profile(Model model) {
		model.addAttribute("title", "Your Profile");

		return "normal/profile";
	}
	
	//setting handler
	@GetMapping("/setting")
	public String setting(Model model) {
		model.addAttribute("title", "Setting");

		return "normal/user_setting";
	}
}