🚀 Social Movie Club: Geliştirme Günlüğü ve Teknik Doküman
Bu doküman, bir fikrin nasıl kurumsal standartlarda bir uygulamaya dönüştüğünü adım adım anlatır.

1. Temel Kurulum ve Teknoloji Seçimleri (Neden Bunları Seçtik?)
   Projeye başlarken modern, performanslı ve sürdürülebilir bir altyapı hedefledik:

Java 17 (LTS): Modern Java özelliklerini (Records, Sealed Classes, yeni String metodları) kullanabilmek ve uzun süreli destek (Long Term Support) almak için seçildi.

Spring Boot 3.x: Jakarta EE geçişi tamamlanmış, daha performanslı ve güvenli olan en güncel Spring sürümü.

PostgreSQL: Verilerimiz arasındaki ilişkiler (Film-Yorum-Kullanıcı) karmaşık olduğu için güçlü bir ilişkisel veritabanı (RDBMS) seçtik.

Docker: Veritabanı ve ileride eklenecek Redis gibi araçların her bilgisayarda aynı şekilde çalışması için "konteyner" yapısını kurduk.

MapStruct: Entity (Veritabanı objesi) ve DTO (Transfer objesi) arasındaki dönüşümü el ile yapıp vakit kaybetmemek ve hata yapmamak için en hızlı Java mapper kütüphanesini seçtik.

2. İlk Adım: Veritabanı ve Ortam Hazırlığı
   İşe kod yazarak değil, kodun yaşayacağı ortamı hazırlayarak başladık:

docker-compose.yml: Proje klasörüne bu dosyayı ekledik. İçine PostgreSQL imajını tanımladık. Böylece docker-compose up komutuyla saniyeler içinde veritabanımız hazır hale geldi.

pom.xml (Bağımlılık Yönetimi): Spring Web, Data JPA, PostgreSQL Driver, Lombok (Kod kalabalığını önlemek için), MapStruct ve Validation kütüphanelerini ekledik.

application.properties: Veritabanı bağlantı bilgilerini ve Spring'in veritabanı tablolarını otomatik oluşturması için hibernate.ddl-auto=update ayarını yaptık.

3. Mimari Tasarım: Çok Dillilik ve Kalıtım (Inheritance)
   Bir "Dünya Sineması" kulübü olduğumuz için filmlerin her dilde farklı ismi ve özeti olmalıydı.

BaseEntity: Her tabloda tekrar eden id, createdAt, updatedAt gibi alanları tek bir yerde topladık. @MappedSuperclass anotasyonu ile diğer sınıfların bu özellikleri "miras" almasını sağladık.

Localization (Çeviri) Mantığı: Filmi ana tablo (Movie), çevirileri yan tablo (MovieTranslation) yaptık.

Neden? Çünkü bir filmin 1 tane çıkış yılı vardır ama 10 tane dilde özeti olabilir. Bu 1-N (Bire-Çok) ilişkidir.

4. Dış Dünya ile Bağlantı: TMDB Entegrasyonu
   Filmleri tek tek elle girmek yerine dünyanın en büyük film arşivi olan TMDB'yi kullandık.

RestTemplate: Java kodumuzun dışarıdaki bir web sitesine (API) gidip veri almasını sağlayan "haberci" aracımız.

TmdbService: Bu sınıfta iş mantığını kurduk:

syncGenres: Önce film türlerini (Aksiyon, Dram vb.) çektik. Veritabanında varsa kaydetmedik (existsByTmdbId), yoksa yeni UUID ile kaydettik.

importMovie: Bir filmi çekerken; önce detaylarını aldık, sonra bizim sistemdeki türlerle eşleştirdik, en son seçilen dile göre çevirisini oluşturup kaydettik.

Hata Yönetimi: Eğer film TMDB'de yoksa veya internet kesilirse sistemin çökmemesi için BusinessException adında kendi hata sınıfımızı yazdık.

5. MapStruct ve Veri Dönüşümü (Mapping)
   Veritabanından gelen karmaşık veriyi (UUID'ler, Setler, Çeviriler) front-end'in anlayacağı basit bir JSON'a dönüştürmemiz gerekiyordu.

@Mapper(componentModel = "spring"): Bu anotasyonla MapStruct'ın bir Spring Bean'i olmasını sağladık.

Akıllı Dil Seçimi (Fallback): Kullanıcı "Türkçe" istiyorsa ve veritabanında Türkçe yoksa, sistemin boş dönmemesi için önce "İngilizce"ye, o da yoksa "bulduğu ilk dile" bakmasını sağlayan özel bir metod yazdık.

6. Güvenlik Devrimi: Spring Security ve JWT
   Sistemi herkese açık olmaktan çıkarıp, "Kulüp üyelerine özel" hale getirdik.

Neden JWT? Mobil uygulamalar ve modern web siteleri için en uygun yöntem budur. Sunucu kullanıcıyı hafızasında tutmaz (Stateless), sadece gelen "anahtarı" (Token) kontrol eder.

BCryptPasswordEncoder: Kullanıcı şifrelerini asla metin olarak kaydetmedik. Şifreyi kırılamaz bir matematiksel özet (Hash) haline getirdik.

AuthTokenFilter: Her gelen isteği durdurup "Senin bir anahtarın var mı?" diye soran bir güvenlik kontrol noktası (Interceptor) ekledik.

SecurityConfig: Kapıları belirledik:

Kayıt ve Giriş: Herkese açık (permitAll).

Filmleri Listeleme: Herkese açık.

Admin İşlemleri: Sadece ROLE_ADMIN yetkisi olanlara.

Yorum ve Profil: Sadece giriş yapmış kullanıcılara (authenticated).

7. İletişim Standardı: RestResponse
   Front-end geliştiricisiyle (veya kendimizle) ortak bir dil oluşturduk.

RestResponse<T>: Tüm API cevaplarını tek bir kutuya koyduk. Bu kutunun içinde;

data: İstediğimiz veri (Film listesi, kullanıcı bilgisi).

success: İşlem başarılı mı? (true/false).

message: Hata kodunu (Örn: 1006) içeren kullanıcı mesajı.

status: HTTP kodu (200, 401 vb.).

8. Sosyal Katman: Hiyerarşik Yorum Sistemi
   Kullanıcıların sadece film izlemekle kalmayıp, birbirleriyle etkileşime girmesini sağlayan "Social Layer" yapısını kurduk.

Self-Referencing Entity (Özyinelemeli İlişki): Instagram ve Discord gibi platformların kullandığı yapıyı tercih ettik. Bir yorumun (Comment), yine bir yorum tipinde parent alanı olmasını sağladık.

parent_id boşsa: Bu bir ana yorumdur.

parent_id doluysa: Bu bir cevaptır (reply).

Recursive Mapping (Özyinelemeli Dönüşüm): MapStruct kütüphanesini kullanarak, bir yorumu çektiğimizde ona bağlı tüm alt yorumların (replies) otomatik olarak iç içe (nested) bir JSON ağacı oluşturmasını sağladık.

Soft Delete: Veri bütünlüğünü korumak için yorumları fiziksel olarak silmedik. deleted flag'ini true yaparak içeriği "Bu mesaj silindi" şeklinde güncelledik. Böylece alt yorumların (cevapların) hiyerarşisi bozulmadan sistemin devamlılığını sağladık.

9. Gelişmiş Hata Yönetimi ve Merkezi Mesaj Sistemi
   Profesyonel bir kullanıcı deneyimi için tüm hata ve başarı mesajlarını bir standart altına aldık.

MessageHelper ve i18n: Mesajları Java kodunun içine gömmek yerine messages.properties dosyasında topladık. Bu sayede uygulamanın ileride farklı dillere (Almanca, Fransızca vb.) çevrilmesini tek bir dosya değişikliğiyle mümkün kıldık.

Hata Kodları (Error Codes): Her hata mesajının başına benzersiz bir kod (Örn: 1016: Session expired) ekledik. Bu sayede front-end ekibi, metne bakmak zorunda kalmadan sadece kod üzerinden kullanıcıya özel uyarılar çıkarabilir hale geldi.

JWT Hata Entegrasyonu: Güvenlik katmanında (JWT) yaşanan hataları (Token süresinin dolması, bozuk token vb.) HttpServletRequest üzerinden AuthEntryPointJwt sınıfına taşıdık. Böylece kullanıcı "Neden giriş yapamadım?" sorusuna net bir cevap alabiliyor.

10. Veri Tutarlılığı ve Hibernate Senkronizasyonu
    Geliştirme sırasında karşılaşılan "ilk kayıtta tarihin null gelmesi" gibi durumları analiz ederek JPA ve Hibernate mekanizmalarını optimize ettik.

JPA Auditing & @PrePersist: Verilerin oluşturulma ve güncellenme tarihlerini @PrePersist ve @PreUpdate metodları ile otomatik hale getirdik.

Persistence Context Yönetimi: Hibernate'in performansı artırmak için kullandığı "gecikmeli yazma" (Write-Behind) stratejisini inceledik. Yapılan testlerde, sistemin doğal akışında (birkaç milisaniyelik senkronizasyon sonrası) verilerin tutarlı hale geldiğini ve save() metodunun ek bir flush yüküne gerek kalmadan görevini başarıyla yerine getirdiğini doğruladık.

Sırada Ne Var?
Sosyal etkileşimi bir adım öteye taşıyarak Rating (Puanlama) Sistemi'ni devreye alıyoruz. Bu sistemle birlikte:

Kullanıcılar filmlere 1-10 arası puan verebilecek.

Aynı kullanıcının aynı filme birden fazla puan vermesi engellenecek (Unique Constraint).

Filmlerin genel puan ortalaması anlık olarak hesaplanacak. adım: Bu güvenli yapının üzerine, üyelerin birbirleriyle etkileşime gireceği Sosyal Yorum Sistemi'ni inşa etmek.

11. Veri Temizliği ve "Mapping Isolation" Standartları
    Sistemin dış API'lerden (TMDB) aldığı verilerin ham (raw) halinden kurtarılıp, kurumsal bir veri modeline dönüştürülmesi için "İzole Mapping" stratejisi uygulandı:

Mapping Contamination (Bulaşma) Çözümü: MapStruct katmanında birden fazla String alanın (Başlık, Poster URL, IMDb ID) bulunması nedeniyle yaşanan yanlış eşleşme riskini @Named anotasyonu ve qualifiedByName parametreleri ile ortadan kaldırdık. Bu sayede, Poster URL oluşturma mantığının yanlışlıkla film başlığına (Title) uygulanması gibi veri kirliliği riskleri mimari düzeyde engellendi.

Akıllı Poster URL Yönetimi: TMDB'nin sunduğu parçalı path yapısını (/path.jpg), sistemimiz otomatik olarak tam bir URL'e (https://image.tmdb.org/...) dönüştürür. Eğer filmde poster bilgisi yoksa, sistem kullanıcıya kırık link yerine profesyonel bir "No Poster Available" placeholder görseli döner.

Atomic Data Structure: originalTitle (evrensel isim) ve title (lokal isim) alanlarını birbirinden ayırarak, veritabanında hem orijinal dilde arama yapabilme kabiliyetini koruduk hem de kullanıcıya kendi dilinde (Localization) hitap ettik.

12. IMDb Entegrasyonu ve Harici Veri Köprüsü
    Uygulamanın bir "film ansiklopedisi" niteliği kazanması için IMDb ekosistemiyle dinamik bir bağ kuruldu:

Dynamic URL Generator (On-the-fly): IMDb tam linklerini veritabanında statik bir metin olarak saklamak yerine, sadece benzersiz imdb_id (Örn: tt1375666) bilgisini saklamayı tercih ettik. Response (Cevap) anında çalışan imdbUrlGenerator sayesinde, IMDb link yapısı değişse bile veritabanına dokunmadan tek satır kodla tüm sistemi güncel tutma esnekliği kazandık.

Fetch Strategy Separation: Performans optimizasyonu amacıyla, hızlı "Arama" (Search) sonuçlarında IMDb detaylarını getirmeyerek ağ trafiğini azalttık. IMDb verisini sadece kullanıcı bir filmi sistemimize "İçe Aktardığında" (Import) derinlemesine çekerek veri bütünlüğünü (Data Integrity) sağladık.




Sıradaki Hedefler:

Rating (Puanlama) Sistemi Sosyal Movie Club'ın en kritik "metric" katmanını inşa ediyoruz:

Rating tablosu ile kullanıcı-film-puan üçgenini kuracağız.

Weighted Average (Ağırlıklı Ortalama): Sadece basit bir aritmetik ortalama değil, ileride kulüp üyelerinin güvenilirliğine göre şekillenebilecek bir puanlama altyapısı hazırlayacağız.

Real-time Update: Bir puan verildiğinde filmin clubRating ve clubVoteCount alanlarının nasıl en verimli şekilde güncelleneceğini (Asenkron vs. Trigger) tartışacağız.