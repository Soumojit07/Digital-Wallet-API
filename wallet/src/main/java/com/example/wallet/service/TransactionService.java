package com.example.wallet.service;

import com.example.wallet.dto.SendMoneyRequest;
import com.example.wallet.dto.TransactionDTO;
import com.example.wallet.model.BankAccount;
import com.example.wallet.model.Transaction;
import com.example.wallet.model.User;
import com.example.wallet.model.Wallet;
import com.example.wallet.repository.BankAccountRepository;
import com.example.wallet.repository.TransactionRepository;
import com.example.wallet.repository.UserRepository;
import com.example.wallet.repository.WalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private BankAccountRepository bankAccountRepository;

    @Autowired
    private WebSocketNotificationService notificationService;

    @Transactional
    public Transaction sendMoney(String senderUsername, SendMoneyRequest request) {
        User sender = userRepository.findByUsername(senderUsername)
                .orElseThrow(() -> new RuntimeException("Sender user not found"));

        Wallet senderWallet = walletRepository.findByUser(sender)
                .orElseThrow(() -> new RuntimeException("Sender wallet not found"));

        BigDecimal amount = request.getAmount();
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Amount must be greater than zero");
        }

        // Validate wallet balance
        if (senderWallet.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient wallet balance");
        }

        // Find receiver by UPI ID, Phone Number or Username
        String target = request.getTargetIdentifier().trim();
        User receiver = userRepository.findByUpiId(target)
                .or(() -> userRepository.findByPhoneNumber(target))
                .or(() -> userRepository.findByUsername(target))
                .orElseThrow(() -> new RuntimeException("Receiver not found with identifier: " + target));

        if (sender.getId().equals(receiver.getId())) {
            throw new RuntimeException("Cannot send money to yourself");
        }

        Wallet receiverWallet = walletRepository.findByUser(receiver)
                .orElseThrow(() -> new RuntimeException("Receiver wallet not found"));

        // Validate Transaction Limits
        validateLimits(sender, senderWallet, amount);

        // Determine transaction type
        boolean isMerchantPayment = receiver.getRole().equalsIgnoreCase("ROLE_MERCHANT");
        String txnType = isMerchantPayment ? "MERCHANT_PAYMENT" : "WALLET_TO_WALLET";

        // Process Balances
        senderWallet.setBalance(senderWallet.getBalance().subtract(amount));
        receiverWallet.setBalance(receiverWallet.getBalance().add(amount));

        // Cashback Calculation (Only for Merchant Payments)
        BigDecimal cashback = BigDecimal.ZERO;
        if (isMerchantPayment) {
            Random random = new Random();
            if (random.nextInt(100) < 30) { // 30% chance for merchant payments
                double percentage = 0.01 + (0.04 * random.nextDouble()); // 1% to 5%
                cashback = amount.multiply(BigDecimal.valueOf(percentage)).setScale(2, RoundingMode.HALF_UP);
                if (cashback.compareTo(new BigDecimal("50.00")) > 0) {
                    cashback = new BigDecimal("50.00");
                }
                if (cashback.compareTo(BigDecimal.ONE) < 0) {
                    cashback = BigDecimal.ONE;
                }
                
                // Credit cashback to sender's wallet
                senderWallet.setBalance(senderWallet.getBalance().add(cashback));
            }
        }

        walletRepository.save(senderWallet);
        walletRepository.save(receiverWallet);

        // Save Transaction
        String transactionId = "TXN" + System.currentTimeMillis() + String.format("%04d", new Random().nextInt(10000));
        Transaction transaction = Transaction.builder()
                .transactionId(transactionId)
                .sender(sender)
                .receiver(receiver)
                .amount(amount)
                .timestamp(LocalDateTime.now())
                .status("SUCCESS")
                .type(txnType)
                .description(request.getDescription() != null ? request.getDescription() : "Transfer to " + receiver.getFullName())
                .cashbackAmount(cashback)
                .build();

        Transaction savedTxn = transactionRepository.save(transaction);

        // Send Real-time Web Socket notifications
        notificationService.sendNotification(sender.getUsername(), 
                String.format("Debit: ₹%s sent to %s. Txn ID: %s", amount, receiver.getFullName(), transactionId));
        
        notificationService.sendNotification(receiver.getUsername(), 
                String.format("Credit: Received ₹%s from %s. Txn ID: %s", amount, sender.getFullName(), transactionId));

        if (cashback.compareTo(BigDecimal.ZERO) > 0) {
            notificationService.sendNotification(sender.getUsername(), 
                    String.format("Cashback Alert! You won ₹%s cashback on your merchant payment!", cashback));
        }

        return savedTxn;
    }

    @Transactional
    public Transaction addFunds(String username, Long bankAccountId, BigDecimal amount) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Wallet wallet = walletRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        BankAccount bankAccount = bankAccountRepository.findById(bankAccountId)
                .orElseThrow(() -> new RuntimeException("Bank account not found"));

        if (!bankAccount.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized bank account mapping");
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Amount must be greater than zero");
        }

        if (bankAccount.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient bank account balance");
        }

        // Deduct from Bank Account, Add to Wallet
        bankAccount.setBalance(bankAccount.getBalance().subtract(amount));
        wallet.setBalance(wallet.getBalance().add(amount));

        bankAccountRepository.save(bankAccount);
        walletRepository.save(wallet);

        // Log Transaction
        String transactionId = "TXN" + System.currentTimeMillis() + String.format("%04d", new Random().nextInt(10000));
        Transaction transaction = Transaction.builder()
                .transactionId(transactionId)
                .sender(null) // From external bank
                .receiver(user)
                .amount(amount)
                .timestamp(LocalDateTime.now())
                .status("SUCCESS")
                .type("BANK_TO_WALLET")
                .description("Add money from bank " + bankAccount.getBankName())
                .cashbackAmount(BigDecimal.ZERO)
                .build();

        Transaction savedTxn = transactionRepository.save(transaction);

        // Notify User
        notificationService.sendNotification(username, 
                String.format("Wallet Loaded: ₹%s added from bank. Balance: ₹%s", amount, wallet.getBalance()));

        return savedTxn;
    }

    @Transactional
    public Transaction withdrawFunds(String username, Long bankAccountId, BigDecimal amount) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Wallet wallet = walletRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        BankAccount bankAccount = bankAccountRepository.findById(bankAccountId)
                .orElseThrow(() -> new RuntimeException("Bank account not found"));

        if (!bankAccount.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized bank account mapping");
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Amount must be greater than zero");
        }

        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient wallet balance");
        }

        // Validate limits
        validateLimits(user, wallet, amount);

        // Deduct from Wallet, Add to Bank Account
        wallet.setBalance(wallet.getBalance().subtract(amount));
        bankAccount.setBalance(bankAccount.getBalance().add(amount));

        walletRepository.save(wallet);
        bankAccountRepository.save(bankAccount);

        // Log Transaction
        String transactionId = "TXN" + System.currentTimeMillis() + String.format("%04d", new Random().nextInt(10000));
        Transaction transaction = Transaction.builder()
                .transactionId(transactionId)
                .sender(user)
                .receiver(null) // Withdraw to external bank
                .amount(amount)
                .timestamp(LocalDateTime.now())
                .status("SUCCESS")
                .type("WALLET_TO_BANK")
                .description("Withdraw money to bank " + bankAccount.getBankName())
                .cashbackAmount(BigDecimal.ZERO)
                .build();

        Transaction savedTxn = transactionRepository.save(transaction);

        // Notify User
        notificationService.sendNotification(username, 
                String.format("Withdrawal: ₹%s transferred to bank. Wallet Balance: ₹%s", amount, wallet.getBalance()));

        return savedTxn;
    }

    public List<TransactionDTO> getTransactionHistory(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Transaction> txns = transactionRepository.findBySenderOrReceiverOrderByTimestampDesc(user, user);
        return txns.stream().map(t -> convertToDTO(t, user)).collect(Collectors.toList());
    }

    public List<TransactionDTO> getMerchantTransactions(String merchantUsername) {
        User merchant = userRepository.findByUsername(merchantUsername)
                .orElseThrow(() -> new RuntimeException("Merchant not found"));

        if (!merchant.getRole().equalsIgnoreCase("ROLE_MERCHANT")) {
            throw new RuntimeException("User is not a merchant");
        }

        List<Transaction> txns = transactionRepository.findByReceiverAndTypeOrderByTimestampDesc(merchant, "MERCHANT_PAYMENT");
        return txns.stream().map(t -> convertToDTO(t, merchant)).collect(Collectors.toList());
    }

    private void validateLimits(User user, Wallet wallet, BigDecimal amount) {
        // Check Per Transaction Limit
        if (amount.compareTo(wallet.getTransactionLimit()) > 0) {
            throw new RuntimeException("Transaction amount exceeds per-transaction limit of ₹" + wallet.getTransactionLimit());
        }

        // Check Daily Limit
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(23, 59, 59);

        List<Transaction> dailyTxns = transactionRepository.findBySenderOrReceiverOrderByTimestampDesc(user, user);
        BigDecimal totalSpentToday = dailyTxns.stream()
                .filter(t -> t.getTimestamp().isAfter(startOfDay) && t.getTimestamp().isBefore(endOfDay))
                .filter(t -> t.getSender() != null && t.getSender().getId().equals(user.getId())) // outgoing only
                .filter(t -> t.getStatus().equalsIgnoreCase("SUCCESS"))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalSpentToday.add(amount).compareTo(wallet.getDailyLimit()) > 0) {
            throw new RuntimeException(String.format(
                    "Transaction exceeds daily transfer limit. Spent today: ₹%s, Limit: ₹%s", 
                    totalSpentToday, wallet.getDailyLimit()));
        }
    }

    private TransactionDTO convertToDTO(Transaction t, User currentUser) {
        return TransactionDTO.builder()
                .transactionId(t.getTransactionId())
                .senderUsername(t.getSender() != null ? t.getSender().getUsername() : null)
                .senderFullName(t.getSender() != null ? t.getSender().getFullName() : "Bank Account")
                .senderUpiId(t.getSender() != null ? t.getSender().getUpiId() : null)
                .receiverUsername(t.getReceiver() != null ? t.getReceiver().getUsername() : null)
                .receiverFullName(t.getReceiver() != null ? t.getReceiver().getFullName() : "Bank Account")
                .receiverUpiId(t.getReceiver() != null ? t.getReceiver().getUpiId() : null)
                .amount(t.getAmount())
                .timestamp(t.getTimestamp())
                .status(t.getStatus())
                .type(t.getType())
                .description(t.getDescription())
                .cashbackAmount(t.getCashbackAmount())
                .build();
    }
}
