package info.kgeorgiy.ja.lorents.student;

import info.kgeorgiy.java.advanced.student.GroupName;
import info.kgeorgiy.java.advanced.student.Student;
import info.kgeorgiy.java.advanced.student.StudentQuery;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.function.BinaryOperator;
import java.util.stream.Stream;

public class StudentDB implements StudentQuery {

    private final static Function<Student, String> GET_FULL_NAME = student -> student.getFirstName() + " " + student.getLastName();
    private final static Comparator<Student> STUDENT_COMPARATOR_BY_NAME = Comparator
            .comparing(Student::getLastName, Comparator.reverseOrder())
            .thenComparing(Student::getFirstName, Comparator.reverseOrder())
            .thenComparing(Student::getId);

    private <T> Stream<T> mapByFunction(List<Student> students, Function<Student, T> function) {
        return students.stream().map(function);
    }

    private <T>List<T> sortByComparator(Collection<T> students, Comparator<? super T> comparator) {
        return students
                .stream()
                .sorted(comparator)
                .collect(Collectors.toList());
    }

    private Stream<Student> findByKey(Collection<Student> students, Function<Student, String> function, String key, Comparator<Student> comparator) {
        return students
                .stream()
                .filter(student -> key.equals(function.apply(student)))
                .sorted(comparator);
    }

    @Override
    public List<String> getFirstNames(List<Student> students) {
        return mapByFunction(students, Student::getFirstName).collect(Collectors.toList());
    }

    @Override
    public List<String> getLastNames(List<Student> students) {
        return mapByFunction(students, Student::getLastName).collect(Collectors.toList());
    }

    @Override
    public List<GroupName> getGroups(List<Student> students) {
        return mapByFunction(students, Student::getGroup).collect(Collectors.toList());
    }

    @Override
    public List<String> getFullNames(List<Student> students) {
        return mapByFunction(students, GET_FULL_NAME).collect(Collectors.toList());
    }

    @Override
    public Set<String> getDistinctFirstNames(List<Student> students) {
        return mapByFunction(students, Student::getFirstName).collect(Collectors.toCollection(TreeSet::new));
    }

    @Override
    public String getMaxStudentFirstName(List<Student> students) {
        return students.stream()
                .max(Student::compareTo)
                .map(Student::getFirstName)
                .orElse("");
    }

    @Override
    public List<Student> sortStudentsById(Collection<Student> students) {
        return sortByComparator(students, Student::compareTo);
    }

    @Override
    public List<Student> sortStudentsByName(Collection<Student> students) {
        return sortByComparator(students, STUDENT_COMPARATOR_BY_NAME);
    }

    @Override
    public List<Student> findStudentsByFirstName(Collection<Student> students, String name) {
        return findByKey(students, Student::getFirstName, name, STUDENT_COMPARATOR_BY_NAME).collect(Collectors.toList());
    }

    @Override
    public List<Student> findStudentsByLastName(Collection<Student> students, String name) {
        return findByKey(students, Student::getLastName, name, STUDENT_COMPARATOR_BY_NAME).collect(Collectors.toList());
    }

    @Override
    public List<Student> findStudentsByGroup(Collection<Student> students, GroupName group) {
        return findByKey(students, student -> student.getGroup().toString(), group.toString(), STUDENT_COMPARATOR_BY_NAME)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(Collection<Student> students, GroupName group) {
        return findByKey(students, student -> student.getGroup().toString(), group.toString(), Comparator.naturalOrder())
                .collect(Collectors.toMap(Student::getLastName, Student::getFirstName, BinaryOperator.minBy(String::compareTo)));
    }
}
