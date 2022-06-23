package info.kgeorgiy.ja.lorents.bank;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class Tests {
    private static final int PORT = 8888;
    private static final String HOST = "//localhost/bank";

    private static Bank bank;
    private final String passportNumber = "1";
    private final String firstName = "Anna";
    private final String lastName = "Lorentz";
    private final String accountName = "lorents";
    private static Registry registry;

    @BeforeClass
    public static void beforeAll() throws RemoteException, MalformedURLException {
        registry = LocateRegistry.createRegistry(PORT);
        createBank();
    }

    @Before
    public void beforeEach() throws RemoteException {
        Bank stub = createBank();
        registry.rebind(HOST, stub);
    }

    private static Bank createBank() throws RemoteException {
        bank = new RemoteBank(PORT);
        return (Bank) UnicastRemoteObject.exportObject(bank, PORT);
    }

    @Test
    public void testCreateAccount() throws RemoteException {
        assertTrue(bank.createPerson(firstName, lastName, passportNumber));
        assertNotNull(bank.getRemotePerson(passportNumber));
        assertTrue(bank.createAccount(accountName, bank.getRemotePerson(passportNumber)));
        Account account = bank.getAccount(accountName, bank.getRemotePerson(passportNumber));
        assertEquals(accountName, account.getId());
        assertEquals(0, account.getAmount());
    }

    // testing getAccountsByPerson
    @Test
    public void testAccountsByPerson() throws RemoteException {
        Random random = new Random();
        for (int i = 0; i < 20; i++) {
            bank.createPerson(firstName, lastName, Integer.toString(i));
            Person person = bank.getRemotePerson(Integer.toString(i));
            int numOfAccount = Math.abs(random.nextInt(20));
            for (int j = 0; j < numOfAccount; j++) {
                bank.createAccount(accountName + j, person);
            }
            Set<String> accounts = bank.getAccountsOfPerson(person);
            assertNotNull(accounts);
            assertEquals(accounts.size(), numOfAccount);
        }
    }

    private void checkParameters(Person person, String i) throws RemoteException {
        assertEquals(firstName + i, person.getFirstName());
        assertEquals(lastName + i, person.getLastName());
        assertEquals(passportNumber + i, person.getPassportNumber());
    }

    // testing createPerson and getRemotePerson and getLocalPerson
    @Test
    public void testCreatePerson() throws RemoteException {
        for (int i = 0; i < 100; i++) {
            String passport = passportNumber.concat(Integer.toString(i));
            assertTrue(bank.createPerson(firstName + i, lastName + i, passport));
            Person remotePerson = bank.getRemotePerson(passport);
            Person localPerson = bank.getLocalPerson(passport);

            checkParameters(remotePerson, Integer.toString(i));
            checkParameters(localPerson, Integer.toString(i));

        }
    }

    // testing create account
    @Test
    public void testCreateManySameAccount() throws RemoteException {
        bank.createPerson(firstName, lastName, passportNumber);
        Person person = bank.getRemotePerson(passportNumber);

        for (int i = 0; i < 10; i++) {
            String subID = accountName + i % 2;
            bank.createAccount(subID, person);
        }
        assertEquals(2, bank.getAccountsOfPerson(person).size());
    }

    @Test
    public void testingBehaviorOfLocateRemoteAccounts() throws RemoteException {
        bank.createPerson(firstName, lastName, passportNumber);
        Person remotePerson = bank.getRemotePerson(passportNumber);

        bank.createAccount(accountName, remotePerson);
        Account remoteAccount = bank.getAccount(accountName, remotePerson);
        Person localPersonFirst = bank.getLocalPerson(passportNumber);

        remoteAccount.setAmount(10);

        Person localPersonSecond = bank.getLocalPerson(passportNumber);

        Account localAccountFirst = bank.getAccount(accountName, localPersonFirst);
        Account localAccountSecond = bank.getAccount(accountName, localPersonSecond);

        assertEquals(0, localAccountFirst.getAmount());
        assertEquals(10, localAccountSecond.getAmount());
    }

    @Test
    public void testingAsyncOfLocalAccount() throws RemoteException {
        bank.createPerson(firstName, lastName, passportNumber);

        Person localPerson1 = bank.getLocalPerson(passportNumber);
        Person localPerson2 = bank.getLocalPerson(passportNumber);

        bank.createAccount(accountName, localPerson1);
        bank.createAccount(accountName + accountName, localPerson2);

        Person localPerson3 = bank.getLocalPerson(passportNumber);

        assertEquals(2, bank.getAccountsOfPerson(localPerson3).size());
        assertEquals(1, bank.getAccountsOfPerson(localPerson1).size());
    }

    @Test
    public void testingSyncOfRemoteAccounts() throws RemoteException {
        bank.createPerson(firstName, lastName, passportNumber);

        Person localPerson1 = bank.getRemotePerson(passportNumber);
        Person localPerson2 = bank.getRemotePerson(passportNumber);

        bank.createAccount(accountName, localPerson1);
        bank.createAccount(accountName + accountName, localPerson2);

        Person localPerson3 = bank.getRemotePerson(passportNumber);

        assertEquals(2, bank.getAccountsOfPerson(localPerson3).size());
        assertEquals(2, bank.getAccountsOfPerson(localPerson2).size());
        assertEquals(2, bank.getAccountsOfPerson(localPerson1).size());
    }

    private void createAccount(int numberOfPersons, int numberOfAccounts) throws RemoteException {
        for (int i = 0; i < numberOfPersons; i++) {
            bank.createPerson(firstName + i, lastName + i, passportNumber + i);
            Person person = bank.getRemotePerson(passportNumber + i);
            for (int j = 0; j < numberOfAccounts; j++) {
                bank.createAccount(accountName + i + j, person);
            }
        }
    }

    @Test
    public void testingMultiThreads() throws RemoteException, InterruptedException {
        final ExecutorService executorService = Executors.newFixedThreadPool(20);
        final int numOfPersons = 5;
        final int numOfAccounts = 3;
        executorService.submit(() -> {
            try {
                createAccount(numOfPersons, numOfAccounts);
            } catch (RemoteException e) {
                // ignore
            }
        });
        close(executorService);
        checkMultithreadingData(numOfPersons, numOfAccounts);
    }

    private void close(ExecutorService executorService) throws InterruptedException {
        executorService.shutdown();
        executorService.awaitTermination(10, TimeUnit.SECONDS);
    }

    private void checkMultithreadingData(int numOfPersons, int numOfAccounts) throws InterruptedException, RemoteException {
        for (int i = 0; i < numOfPersons; i++) {
            assertNotNull(bank.getRemotePerson(passportNumber + i));
            Person person = bank.getRemotePerson(passportNumber + i);
            assertEquals(numOfAccounts, bank.getAccountsOfPerson(person).size());
            assertEquals(firstName + i, person.getFirstName());
            assertEquals(lastName + i, person.getLastName());
        }
    }

    @Test
    public void testingManyThreads() throws RemoteException, InterruptedException {
        final int numOfThreads = 20;
        final int numOfAccounts = 5;
        final ExecutorService executorService = Executors.newFixedThreadPool(numOfThreads);
        for (int thread = 0; thread < numOfThreads; thread++) {
            int finalThread = thread;
            executorService.submit(() -> {
                try {
                    bank.createPerson(firstName + finalThread, lastName + finalThread, passportNumber + finalThread);
                    Person person = bank.getRemotePerson(passportNumber + finalThread);
                    for (int account = 0; account < numOfAccounts; account++) {
                        bank.createAccount(accountName + account, person);
                    }
                } catch (RemoteException e) {
                    // ignore
                }
            });
        }
        close(executorService);
        checkMultithreadingData(numOfThreads, numOfAccounts);
    }
}

