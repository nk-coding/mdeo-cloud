// Example metamodels that respect the defined grammar rules

export const LIBRARY_METAMODEL = `
class Book {
    title: string
    isbn: string
    publicationYear: int
    pageCount: int
}

class Author {
    name: string
    birthYear: int
    nationality: string
}

class Publisher {
    name: string
    address: string
    foundedYear: int
}

class Library {
    name: string
    location: string
    capacity: int
}

Book.author -- Author.books[*]
Book.publisher -- Publisher.publishedBooks[*]
Library.books[*] *-- Book
`;

export const E_COMMERCE_METAMODEL = `
class abstract User {
    id: int
    email: string
    name: string
    registrationDate: string
}

class Customer {
    loyaltyPoints: int
    preferredPaymentMethod: string
}

class Admin {
    permissions: string[*]
    department: string
}

class Product {
    id: int
    name: string
    price: double
    description: string
    stockQuantity: int
}

class Order {
    id: int
    orderDate: string
    totalAmount: double
    status: string
}

class OrderItem {
    quantity: int
    unitPrice: double
}

Customer.orders[*] -- Order.customer
Order.items[1..*] *-- OrderItem
OrderItem.product -- Product.orderItems[*]
`;

export const UNIVERSITY_METAMODEL = `
class Person {
    firstName: string
    lastName: string
    dateOfBirth: string
    email: string
}

class Student {
    studentId: string
    enrollmentYear: int
    gpa: float
}

class Professor {
    employeeId: string
    department: string
    tenure: boolean
}

class Course {
    courseCode: string
    title: string
    credits: int
    description: string
}

class Enrollment {
    semester: string
    year: int
    grade: string
}

class Department {
    name: string
    building: string
    budget: double
}

Student.enrollments[*] *-- Enrollment
Enrollment.course -- Course.enrollments[*]
Professor.taughtCourses[*] -- Course.instructor
Department.professors[*] *-- Professor
Department.courses[*] *-- Course
`;

export const BLOG_PLATFORM_METAMODEL = `
class User {
    username: string
    email: string
    passwordHash: string
    joinDate: string
    isActive: boolean
}

class BlogPost {
    id: int
    title: string
    content: string
    publishDate: string
    isPublished: boolean
    viewCount: int
}

class Comment {
    id: int
    content: string
    timestamp: string
    isApproved: boolean
}

class Tag {
    name: string
    description: string
    color: string
}

class Category {
    name: string
    description: string
    parentCategory: string
}

User.posts[*] -- BlogPost.author
BlogPost.comments[*] *-- Comment
Comment.author -- User.comments[*]
BlogPost.tags[*] -- Tag.posts[*]
BlogPost.category -- Category.posts[*]
`;
