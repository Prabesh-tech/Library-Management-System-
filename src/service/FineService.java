package service;

import model.Fine;
import repository.FineRepository;
import util.DataManager;
import java.util.List;

/**
 * FineService.java
 * Business logic for fine tracking and payment.
 */
public class FineService {

    private FineRepository fineRepository;

    public FineService() {
        this.fineRepository = DataManager.getInstance().getFineRepository();
    }

    public boolean createFine(Fine fine) {
        return fineRepository.save(fine);
    }

    public Fine getFine(String fineId) {
        return fineRepository.findById(fineId);
    }

    public boolean updateFine(Fine fine) {
        return fineRepository.update(fine);
    }

    public boolean markFinePaid(String fineId, String method) {
        Fine fine = fineRepository.findById(fineId);
        if (fine == null || fine.isPaid()) return false;
        fine.markPaid(method);
        return fineRepository.update(fine);
    }

    public boolean markFinePaidByLoanId(String loanId, String method) {
        Fine fine = fineRepository.findByLoanId(loanId);
        if (fine == null || fine.isPaid()) return false;
        fine.markPaid(method);
        return fineRepository.update(fine);
    }

    public Fine getFineByLoanId(String loanId) {
        return fineRepository.findByLoanId(loanId);
    }

    public List<Fine> getUserFines(String userId) {
        return fineRepository.findByUserId(userId);
    }

    public List<Fine> getUnpaidFines() {
        return fineRepository.findUnpaid();
    }

    public List<Fine> getAllFines() {
        return fineRepository.findAll();
    }
}
