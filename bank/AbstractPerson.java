package info.kgeorgiy.ja.lorents.bank;


public class AbstractPerson implements Person{

    private final String firstName;
    private final String lastName;
    private final String passportNumber;

    /**
     * Creates a {@link AbstractPerson} by his first name, last name and passport
     * @param firstName {@link String}
     * @param lastName {@link String} of surname
     * @param passportNumber {@link String}
     */
    public AbstractPerson(String firstName, String lastName, String passportNumber) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.passportNumber = passportNumber;
    }

    @Override
    public String getFirstName() {
        return firstName;
    }

    @Override
    public String getLastName() {
        return lastName;
    }

    @Override
    public String getPassportNumber() {
        return passportNumber;
    }

}
