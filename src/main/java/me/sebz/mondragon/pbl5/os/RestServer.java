package me.sebz.mondragon.pbl5.os;

public class RestServer {
    
    private Database database;

    public RestServer() {
        database = new Database();
    }

    private Group getValidatedGroup(Long sessionId) {
        Group group = database.getGroupFromSession(sessionId).join();
        if (group == null) {
            System.out.println("Invalid session ID: " + sessionId);
        }
        return group;
    }

    private Person getValidatedPerson(Long sessionId, int personId) {
        Group group = getValidatedGroup(sessionId);
        if (group == null) {
            return null;
        }
        Person person = group.getMemberById(personId);
        if (person == null) {
            System.out.println("Person ID: " + personId + " not found in group ID: " + group.getId());
        }
        return person;
    }

    public Long newSession(int groupId, String password) {
        Long sessionId = database.signIn(groupId, password).join();
        if (sessionId != null) {
            System.out.println("New session created with ID: " + sessionId);
        } else {
            System.out.println("Failed to create session for group ID: " + groupId);
        }
        return sessionId;
    }

    public int createGroup(String password) {
        Group group = new Group();
        group.setPassword(password);
        database.addGroup(group);
        System.out.println("Created new group: with ID: " + group.getId());
        return group.getId();
    }

    public void deleteGroup(Long sessionId) {
        Group group = getValidatedGroup(sessionId);
        if (group == null) {
            return;
        }
        database.removeGroup(group);
        System.out.println("Deleted group with ID: " + group.getId());
    }

    public int createPerson(Long sessionId, String info, float[] faceEmbedding) {
        Group group = getValidatedGroup(sessionId);
        if (group == null) {
            return -1;
        }
        Person person = new Person();
        person.setInfo(info);
        person.setFaceEmbedding(faceEmbedding);
        group.addMember(person);
        System.out.println("Created new person with ID: " + person.getId() + " in group ID: " + group.getId());
        return person.getId();
    }

    public void deletePerson(Long sessionId, int personId) {
        Group group = getValidatedGroup(sessionId);
        if (group == null) {
            return;
        }
        group.removeMember(personId);
        System.out.println("Deleted person with ID: " + personId + " from group ID: " + group.getId());
    }

    public void editPersonInfo(Long sessionId, int personId, String newInfo) {
        Person person = getValidatedPerson(sessionId, personId);
        if (person == null) {
            return;
        }
        person.setInfo(newInfo);
        System.out.println("Updated info for person ID: " + personId + " in group ID: " + database.getGroupFromSession(sessionId).join().getId());
    }

    public void editPersonEmbedding(Long sessionId, int personId, float[] newEmbedding) {
        Person person = getValidatedPerson(sessionId, personId);
        if (person == null) {
            return;
        }
        person.setFaceEmbedding(newEmbedding);
        System.out.println("Updated face embedding for person ID: " + personId + " in group ID: " + database.getGroupFromSession(sessionId).join().getId());
    }

    private Person findClosestPerson(Long sessionId, float[] embedding) {
        Group group = getValidatedGroup(sessionId);
        if (group == null) {
            return null;
        }
        Person closestPerson = group.getClosestMember(embedding);
        if (closestPerson != null) {
            System.out.println("Found closest person with ID: " + closestPerson.getId() + " in group ID: " + group.getId());
        } else {
            System.out.println("No members with face embeddings found in group ID: " + group.getId());
        }
        return closestPerson;
    }

    public String identifyPerson(Long sessionId, float[] embedding) {
        Person closestPerson = findClosestPerson(sessionId, embedding);
        if (closestPerson != null) {
            return closestPerson.getInfo();
        }
        return null;
    }
}
