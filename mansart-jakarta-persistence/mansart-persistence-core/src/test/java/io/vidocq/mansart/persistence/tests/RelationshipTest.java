/*
 * Copyright (c) 2026 Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.tests;

import io.vidocq.mansart.persistence.tests.model.relationship.Author;
import io.vidocq.mansart.persistence.tests.model.relationship.Book;
import io.vidocq.mansart.persistence.tests.model.relationship.Course;
import io.vidocq.mansart.persistence.tests.model.relationship.Pupil;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for relationship mappings (M8-10).
 * Phase 1: Basic persist and find operations with OneToMany and ManyToMany relationships.
 */
public class RelationshipTest extends BasePersistenceTest {

    // ========== OneToMany Tests ==========

    @Test
    public void testPersistAndFindAuthor() {
        Author author = new Author("J.K. Rowling");
        
        em.persist(author);
        assertThat(author.getId()).isNotNull();
        
        Author found = em.find(Author.class, author.getId());
        assertThat(found).isNotNull();
        assertThat(found.getName()).isEqualTo("J.K. Rowling");
    }

    @Test
    public void testPersistAndFindBook() {
        Book book = new Book("Harry Potter");
        
        em.persist(book);
        assertThat(book.getId()).isNotNull();
        
        Book found = em.find(Book.class, book.getId());
        assertThat(found).isNotNull();
        assertThat(found.getTitle()).isEqualTo("Harry Potter");
    }

    @Test
    public void testOneToManyPersistWithAuthorAndBook() {
        Author author = new Author("George Orwell");
        Book book = new Book("1984");
        
        // Set up bidirectional relationship
        book.setAuthor(author);
        author.addBook(book);
        
        em.persist(author);
        em.persist(book);
        
        assertThat(author.getId()).isNotNull();
        assertThat(book.getId()).isNotNull();
        
        // Find both entities
        Author foundAuthor = em.find(Author.class, author.getId());
        Book foundBook = em.find(Book.class, book.getId());
        
        assertThat(foundAuthor).isNotNull();
        assertThat(foundBook).isNotNull();
        assertThat(foundBook.getAuthor()).isNotNull();
    }

    // ========== ManyToMany Tests ==========

    @Test
    public void testPersistAndFindPupil() {
        Pupil pupil = new Pupil("Alice");
        
        em.persist(pupil);
        assertThat(pupil.getId()).isNotNull();
        
        Pupil found = em.find(Pupil.class, pupil.getId());
        assertThat(found).isNotNull();
        assertThat(found.getName()).isEqualTo("Alice");
    }

    @Test
    public void testPersistAndFindCourse() {
        Course course = new Course("Mathematics");
        
        em.persist(course);
        assertThat(course.getId()).isNotNull();
        
        Course found = em.find(Course.class, course.getId());
        assertThat(found).isNotNull();
        assertThat(found.getTitle()).isEqualTo("Mathematics");
    }

    @Test
    public void testManyToManyPersistWithPupilAndCourse() {
        Pupil pupil = new Pupil("Bob");
        Course course = new Course("Physics");
        
        // Set up bidirectional relationship
        pupil.addCourse(course);
        
        em.persist(pupil);
        em.persist(course);
        
        assertThat(pupil.getId()).isNotNull();
        assertThat(course.getId()).isNotNull();
        
        // Find both entities
        Pupil foundPupil = em.find(Pupil.class, pupil.getId());
        Course foundCourse = em.find(Course.class, course.getId());
        
        assertThat(foundPupil).isNotNull();
        assertThat(foundCourse).isNotNull();
    }
}
