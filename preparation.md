---
layout: page
title: Preparation
---

## 1st presentation

<div><iframe class="speakerdeck-iframe" frameborder="0" src="//speakerdeck.com/player/52b120ee449846be8339f791c5f9f737?" allowfullscreen="true" mozallowfullscreen="true" webkitallowfullscreen="true" style="border: 0px; margin: 0px; padding: 0px; border-radius: 5px; width: 710px; height: 461.37499999999955px; background: transparent;"></iframe></div>

*Presented Dec 3, 2015*

## 2nd presentation

<div><iframe class="speakerdeck-iframe" frameborder="0" src="//speakerdeck.com/player/aa0b6292006542c285bbace9533ac897?" allowfullscreen="true" mozallowfullscreen="true" webkitallowfullscreen="true" style="border: 0px; margin: 0px; padding: 0px; border-radius: 5px; width: 710px; height: 461.37499999999955px; background: transparent;"></iframe></div>

*Presented Feb 3, 2016*

## Proposal

### Context

E-commerce websites rely heavily on summarizing and analyzing the 
behavior of customers, trying to influence user actions towards the 
optimization of success metrics such as CTR (Click through Rate), CPC 
(Cost per Conversion), Basket and Lifetime Value and User Engagement.
Data mining and machine learning techniques 
have been applied as tools of personal service in websites, making great 
significance in Internet marketing activities. 

The successful experimentation and application of such techniques is 
highly dependent on the methods of online and offline evaluation. 
Measuring how much certain actions influence the user behavior is 
particularly tricky by just relying on historical data, because the 
causality aspects of the system are not taken into consideration. For 
example, a user might have an affinity towards sports, but because 
historically he was never exposed to recommendations in that category, 
one cannot predict his behavior when presented with such suggestion 
(it's hard to extrapolate patterns that never occurred before). Data 
scientists would usually have to resort to online techniques, such as 
A/B testing or multi-armed bandit optimization, to consider new areas of 
the hypothesis space, and therefore incurring into the operational cost 
of such exploration. Most of the cases, however, marketing is not a pure 
blank slate. There exists a body of knowledge of known mass behavior, 
personas and their affinities, that have been (and still are) collected 
through (meta-)studies. 

### Objectives and expected results 

The goal of this dissertation is to allow the usage of such "a priori" 
knowledge in a bayesian setting, by regarding users as agents that simulate 
personas interacting on an e-commerce site and react to stimuli that 
influence their choices and actions. Hence we propose to create a 
framework that allows the construction of simulators of user activity in 
e-commerce sites, enabling the evaluation of "what-if" scenarios. This 
framework should take as input data from web mining (*Web structure 
mining* (WSM), *Web usage mining* (WUM) and *Web content 
mining* (WCM)), which includes both static and 
dynamic content of websites as well as user profiles or 
the rate that new users visit a website (e.g by modeling it as a Poisson 
model). Additionally, multiple parameters to 
configure the simulation have to be provided (e.g running time of the 
simulation or the number of users to generate). The results should 
include multiple metrics gathered during the simulation so that the 
hypothesis can be validated. A planned use case of our framework is to 
evaluate the effectiveness of a product recommender engine, e.g how can 
different recommendation algorithms affect the probability that a 
product is bought by a customer. 

It is worth noting that the dissertation entitled *Reverse 
Engineering Static Content and Dynamic Behavior of E-Commerce Sites for 
Fun and Profit* complements this framework, allowing to fuel our 
simulation with real data and rules. The integration of these two tools 
is not the main objective of our work. 

### Innovative aspects

As far as the authors know, there's currently no similar tool available in the market.

### Provisory work plan

* Research state of the art 
* Implement small exploratory prototypes in order to assess (and get an initial feeling of) key features and techniques 
* Design experimental scenario(s) with key emphasis on reproducibility 
* Based on the previous point, redefine strategy and goals for a 6mo development effort 
* Assemble experimental package and proceed with experimental evaluation 
* Dissertation writing period (1mo)

### Bibliography references

[[G  O03](http://web.itu.edu.tr/sgunduz/papers/iscis.pdf)] Sule Gunduz and M Tamer  Ozsu. A poisson model for user accesses
to web pages. In Computer and Information Sciences-ISCIS 2003,
pages 332{339. Springer, 2003.

[[LJZ08](http://ieeexplore.ieee.org/xpl/login.jsp?tp=&arnumber=4783589&url=http%3A%2F%2Fieeexplore.ieee.org%2Fxpls%2Fabs_all.jsp%3Farnumber%3D4783589)] Weilong Liu, Fang Jin, and Xin Zhang. Ontology-based user modeling
for e-commerce system. In Pervasive Computing and Applications,
2008. ICPCA 2008. Third International Conference on, volume 1,
pages 260{263, Oct 2008.

[[NMK14](http://www.ijcsi.org/papers/IJCSI-11-6-2-144-152.pdf)] Wamukekhe Everlyne Nasambu, Waweru Mwangi, and Michael
Kimwele. Predicting sales in e-commerce using bayesian network
model. International Journal of Computer Science Issues (IJCSI),
11(6):144, 2014.

[[PV12](http://ceur-ws.org/Vol-910/paper11.pdf)] Ladislav Peska and Peter Vojtas. Evaluating various implicit factors
in e-commerce. RUE (RecSys), pages 51{55, 2012.

[[RJM](https://rjmetrics.com/resources/reports/ecommerce-buyer-behavior)] RJMetrics. Ecommerce buyer behavior. [https://rjmetrics.com/resources/reports/ecommerce-buyer-behavior](https://rjmetrics.com/resources/reports/ecommerce-buyer-behavior). Accessed: 2015-10-06.

[[SB04](http://weigend.com/files/teaching/stanford/2007/readings/ModelingPurchaseBehaviorTaskCompletion%2520SismeiroBucklin%2520JMarketingResearch2004.pdf)] Catarina Sismeiro and Randolph E Bucklin. Modeling purchase behavior
at an e-commerce web site: A task-completion approach. Journal of marketing
research, 41(3):306{323, 2004.

[[Vak06](http://www.igi-global.com/book/web-data-management-practices/1042)] A. Vakali. Web Data Management Practices: Emerging Techniques
and Technologies: Emerging Techniques and Technologies. Gale Virtual
Reference Library. Idea Group Pub., 2006.
