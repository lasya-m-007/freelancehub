package com.freelancehub.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.freelancehub.exception.ForbiddenException;
import com.freelancehub.exception.InvalidBidException;
import com.freelancehub.model.Bid;
import com.freelancehub.model.BidStatus;
import com.freelancehub.model.Project;
import com.freelancehub.model.ProjectStatus;
import com.freelancehub.model.Role;
import com.freelancehub.model.User;
import com.freelancehub.repository.BidRepository;
import com.freelancehub.repository.ProjectRepository;
import com.freelancehub.repository.UserRepository;
import com.freelancehub.service.impl.BidServiceImpl;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BidServiceImplTest {

    @Mock private BidRepository bidRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private UserRepository userRepository;
    @InjectMocks private BidServiceImpl service;

    private User freelancer;
    private User client;
    private Project project;

    private static User user(Long id, Role role) {
        User u = new User();
        u.setId(id);
        u.setRole(role);
        u.setName("user" + id);
        return u;
    }

    @BeforeEach
    void setUp() {
        freelancer = user(1L, Role.FREELANCER);
        client = user(2L, Role.CLIENT);
        project = new Project();
        project.setId(10L);
        project.setClient(client);
        project.setBudget(new BigDecimal("1000"));
        project.setStatus(ProjectStatus.OPEN);

        when(userRepository.findById(1L)).thenReturn(Optional.of(freelancer));
        when(userRepository.findById(2L)).thenReturn(Optional.of(client));
        when(projectRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(project));
        when(bidRepository.save(any(Bid.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void placeBid_rejectsAmountAboveBudget() {
        assertThrows(InvalidBidException.class,
                () -> service.placeBid(1L, 10L, new BigDecimal("1000.01"), "hello"));
        verify(bidRepository, never()).save(any(Bid.class));
    }

    @Test
    void placeBid_rejectsClosedProject() {
        project.setStatus(ProjectStatus.CLOSED);
        assertThrows(InvalidBidException.class,
                () -> service.placeBid(1L, 10L, new BigDecimal("500"), "hello"));
    }

    @Test
    void placeBid_rejectsNonFreelancer() {
        assertThrows(ForbiddenException.class,
                () -> service.placeBid(2L, 10L, new BigDecimal("500"), "hello"));
    }

    @Test
    void placeBid_savesValidBidAsPending() {
        Bid bid = service.placeBid(1L, 10L, new BigDecimal("500"), "  I can do this  ");
        assertEquals(BidStatus.PENDING, bid.getStatus());
        assertEquals("I can do this", bid.getMessage());
    }

    @Test
    void acceptBid_acceptsOneRejectsOthersAndStartsProject() {
        User other = user(3L, Role.FREELANCER);
        Bid winner = bid(100L, freelancer);
        Bid loser = bid(101L, other);
        when(bidRepository.findById(100L)).thenReturn(Optional.of(winner));
        when(bidRepository.findByProjectId(10L)).thenReturn(List.of(winner, loser));

        service.acceptBid(100L, 2L);

        assertEquals(BidStatus.ACCEPTED, winner.getStatus());
        assertEquals(BidStatus.REJECTED, loser.getStatus());
        assertEquals(ProjectStatus.IN_PROGRESS, project.getStatus());
    }

    @Test
    void acceptBid_onlyProjectOwnerMayAccept() {
        Bid bid = bid(100L, freelancer);
        when(bidRepository.findById(100L)).thenReturn(Optional.of(bid));
        assertThrows(ForbiddenException.class, () -> service.acceptBid(100L, 99L));
    }

    private Bid bid(Long id, User owner) {
        Bid b = new Bid();
        b.setId(id);
        b.setProject(project);
        b.setFreelancer(owner);
        b.setAmount(new BigDecimal("500"));
        b.setMessage("msg");
        b.setStatus(BidStatus.PENDING);
        return b;
    }
}
