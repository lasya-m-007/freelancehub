package com.freelancehub.controller.api;

import com.freelancehub.dto.BidDto;
import com.freelancehub.dto.BidRequest;
import com.freelancehub.exception.ForbiddenException;
import com.freelancehub.model.Bid;
import com.freelancehub.model.Role;
import com.freelancehub.security.AuthHelper;
import com.freelancehub.security.SessionUser;
import com.freelancehub.service.BidService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bids")
public class BidApiController {

    private final BidService bidService;
    private final AuthHelper auth;

    public BidApiController(BidService bidService, AuthHelper auth) {
        this.bidService = bidService;
        this.auth = auth;
    }

    /** Admin: all bids; Client: bids on own projects; Freelancer: own bids. */
    @GetMapping
    public List<BidDto> list(HttpServletRequest request) {
        SessionUser user = auth.requireLogin(request);
        return bidService.findForUser(user.getId(), user.getRole()).stream().map(BidDto::from).toList();
    }

    @GetMapping("/{id}")
    public BidDto get(@PathVariable Long id, HttpServletRequest request) {
        SessionUser user = auth.requireLogin(request);
        Bid bid = bidService.findById(id);
        boolean allowed = user.isAdmin()
                || bid.getFreelancer().getId().equals(user.getId())
                || bid.getProject().getClient().getId().equals(user.getId());
        if (!allowed) throw new ForbiddenException("You cannot view this bid");
        return BidDto.from(bid);
    }

    @PostMapping
    public ResponseEntity<BidDto> create(@Valid @RequestBody BidRequest req, HttpServletRequest request) {
        SessionUser user = auth.requireRole(request, Role.FREELANCER);
        Bid bid = bidService.placeBid(user.getId(), req.projectId(), req.amount(), req.message());
        return ResponseEntity.status(HttpStatus.CREATED).body(BidDto.from(bid));
    }

    @PutMapping("/{id}")
    public BidDto update(@PathVariable Long id, @Valid @RequestBody BidRequest req, HttpServletRequest request) {
        SessionUser user = auth.requireRole(request, Role.FREELANCER);
        return BidDto.from(bidService.updateBid(id, user.getId(), req.amount(), req.message()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        SessionUser user = auth.requireRole(request, Role.FREELANCER, Role.ADMIN);
        bidService.delete(id, user.getId(), user.getRole());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/accept")
    public BidDto accept(@PathVariable Long id, HttpServletRequest request) {
        SessionUser user = auth.requireRole(request, Role.CLIENT);
        return BidDto.from(bidService.acceptBid(id, user.getId()));
    }
}
