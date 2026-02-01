DROP DATABASE IF EXISTS CourseRegistration;
CREATE DATABASE CourseRegistration CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE CourseRegistration;


create table Course(
  courseID char(5),
  subjectID char(4) not null, 
  courseNum integer not null, 
  title varchar(50) not null,
  numCredit integer, 
  primary key (courseID) 
);


create table Student(
 ssn char(9),
 fName varchar(25) not null, 
 mi char(1),                 
 lName varchar(25) not null, 
 bDate date,
 street varchar(25),
 phone char(11),
 zipCode char(5),
 deptId char(4),             
 primary key (ssn)
);


create table Enrollment(
 ssn char(9),
 courseID char(5),
 dateReg date not null,      
 grade char(1),              
 

 foreign key (ssn) references Student(ssn) ON DELETE RESTRICT,
 foreign key (courseID) references Course(courseID) ON DELETE RESTRICT,
 
 CONSTRAINT pk_Enrollment PRIMARY KEY (ssn, courseID) 
);


insert into Course values( 'cs482','CSEE','482','advanced java',3);
insert into Course values('11111','CSCI','1301','Introduction to Java I',4);
insert into Course values('11112','CSCI','1302','Introduction to Java II',3);
insert into Course values('11113','CSCI','3720','Database Systems',3);
insert into Course values('11114','CSCI','4750','Rapid Java Application',3);
insert into Course values('11115','MATH','2750','Calculus I',5);
insert into Course values('11116','MATH','3750','Calculus II',5);
insert into Course values('11117','EDUC','1111','Reading',3);
insert into Course values('11118','ITEC','1344','Database Administration',3);


insert into Student values( '444111110','Jacob','R','Smith','1985-04-09','Kingston Street','9129219434','31435','BIOL');
insert into Student values( '444111111','John','K','Stevenson',null,'Main Street','9129219434','31411','BIOL');
insert into Student values( '444111112','George','K','Smith','1974-10-10','Abercorn St.','9129213454','31419','CS');
insert into Student values( '444111113','Frank','E','Jones','1970-09-09','Main Street','9125919434','31411','BIOL');
insert into Student values( '444111114','Jean','K','Smith','1970-02-09','Main Street','9129219434','31411','CHEM');
insert into Student values( '444111115','Josh','R','Woo','1970-02-09','Franklin St.','7075989434','31411','CHEM');
insert into Student values( '444111116','Josh','R','Smith','1973-02-09','Main Street','9129219434','31411','BIOL');
insert into Student values( '444111117','Joy','P','Kennedy','1974-03-19','Bay Street','9129229434','31412','CS');
insert into Student values( '444111118','Toni','R','Peterson','1964-04-29','Bay Street','9129229434','31412','MATH');
insert into Student values( '444111119','Patrick','R','Stoneman','1969-04-29','Washington St.','9129229434','31435','MATH');
insert into Student values( '444111120','Rick','R','Carter','1986-04-09','West Ford St.','9125919434','31411','BIOL');


insert into Enrollment values('444111110','11111','2004-03-19','A');
insert into Enrollment values('444111110','11112','2004-03-19','B');
insert into Enrollment values('444111110','11113','2004-03-19','C');
insert into Enrollment values('444111111','11111','2004-03-19','D');
insert into Enrollment values('444111111','11112','2004-03-19','F');
insert into Enrollment values('444111111','11113','2004-03-19','A');
insert into Enrollment values('444111112','11114','2004-03-19','B');
insert into Enrollment values('444111112','11115','2004-03-19','C');
insert into Enrollment values('444111112','11116','2004-03-19','D');
insert into Enrollment values('444111113','11111','2004-03-19','A');
insert into Enrollment values('444111113','11113','2004-03-19','A');