package com.freelancehub.controller.web;

import com.freelancehub.model.Bid;
import com.freelancehub.model.BidStatus;
import com.freelancehub.model.Role;
import com.freelancehub.security.AuthHelper;
import com.freelancehub.security.SessionUser;
import com.freelancehub.service.BidService;
import com.freelancehub.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/freelancer")
public class FreelancerController {

    private final BidService bidService;
    private final UserService userService;
    private final AuthHelper auth;

    public FreelancerController(BidService bidService, UserService userService, AuthHelper auth) {
        this.bidService = bidService;
        this.userService = userService;
        this.auth = auth;
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpServletRequest request, Model model) {
        SessionUser user = auth.requireRole(request, Role.FREELANCER);
        List<Bid> bids = bidService.findByFreelancer(user.getId());
        Map<BidStatus, Long> byStatus = bids.stream()
                .collect(Collectors.groupingBy(Bid::getStatus, Collectors.counting()));

        model.addAttribute("me", userService.findById(user.getId()));
        model.addAttribute("bids", bids);
        model.addAttribute("totalBids", bids.size());
        model.addAttribute("pendingCount", byStatus.getOrDefault(BidStatus.PENDING, 0L));
        model.addAttribute("acceptedCount", byStatus.getOrDefault(BidStatus.ACCEPTED, 0L));
        model.addAttribute("rejectedCount", byStatus.getOrDefault(BidStatus.REJECTED, 0L));
        return "freelancer/dashboard";
    }
}
