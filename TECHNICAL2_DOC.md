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


13. Puanlama Sistemi ve Dinamik İstatistik Yönetimi (Rating Engine)
    Social Movie Club’ın "sosyal" kimliğini oluşturan puanlama altyapısı, yüksek performanslı bir denormalizasyon stratejisi üzerine kurulmuştur.

Upsert (Update or Insert) Mekanizması: Kullanıcı deneyimini kesintisiz kılmak adına sistem "Upsert" mantığıyla çalışır. Bir kullanıcı bir filme puan verdiğinde, sistem önce veritabanında o kullanıcıya ait eski bir kayıt olup olmadığını kontrol eder. Varsa mevcut kayıt güncellenir (Update), yoksa yeni bir kayıt oluşturulur (Insert). Bu sayede her film için kullanıcı başına tekil bir veri seti (Unique Constraint) garanti altına alınır.

Triggered Aggregation (Tetiklemeli Hesaplama): Puanlama işlemi sadece bir tablonun güncellenmesi değildir. Bir Rating kaydedildiği anda, sistem otomatik olarak bir "istatistik tazeleme" sürecini başlatır:

Veritabanı seviyesinde AVG(score) ve COUNT(id) fonksiyonları çalıştırılarak o filmin en güncel ortalaması ve toplam oy sayısı hesaplanır.

Bu hesaplanan değerler, anlık olarak Movie entity'si üzerindeki clubRating ve clubVoteCount alanlarına yazılır.

Eager vs. Lazy Trade-off: Bu mimari, "Yazma anında hesapla, okuma anında hazır sun" prensibini (Denormalizasyon) benimser. Binlerce kullanıcı filmleri listelerken (Read) ağır matematiksel işlemler yapılmaz; veriler Movie tablosunda önceden hesaplanmış (Pre-calculated) şekilde bekler.

14. Teknik Kararlar ve Güvenlik Standartları
    Puanlama servisinin inşasında uygulanan kritik mühendislik kararları:

Security Context Integration: Puanlama isteği (RatingRequest) içerisinde asla userId kabul edilmez. Kullanıcı kimliği, doğrudan JWT üzerinden SecurityContextHolder aracılığıyla sunucu tarafında çözümlenir. Bu, bir kullanıcının başkası adına puan vermesini (ID Spoofing) mimari olarak imkansız kılar.

Transactional Integrity (@Transactional): Puanın kaydedilmesi ve film istatistiklerinin güncellenmesi tek bir atomik işlem olarak tanımlanmıştır. Eğer istatistik güncellenirken bir hata oluşursa, verilen puan da geri alınır (Rollback). Bu sayede veritabanındaki "puanlar toplamı" ile "filmin üzerinde yazan ortalama" arasındaki tutarlılık asla bozulmaz.

Type Casting & SQL Precision: Repository seviyesinde yapılan Object[] dönüşümleri ve Double/Long casting işlemleri, SQL motorundan gelen ham verinin Java tip güvenliği (Type Safety) kurallarına uygun şekilde işlenmesini sağlar.

15. Frontend-Backend Senkronizasyonu (UUID Kullanımı)
    Sistem, son kullanıcıyı teknik detaylarla yormadan arka planda karmaşık bir ID yönetimi yürütür:

UUID Abstraction: Kullanıcı arayüzde (Frontend) sadece film afişlerini ve yıldızları görür. Ancak her etkileşimde, afişin arkasına gizlenmiş olan 128-bitlik benzersiz UUID (movieId) arka planda API isteklerine eklenir.

Decoupled Architecture: Dış dünyadan gelen TMDB ID'ler sadece birer referanstır; sistemin tüm iç damarları (Yorumlar, Puanlar, Listeler) bizim ürettiğimiz UUID'ler üzerinden haberleşir. Bu, yarın başka bir veri kaynağına geçilse bile sistemin iskeletinin sarsılmamasını sağlar.


16. Akıllı Film Keşfi ve Sıralama Algoritmaları (Discovery Layer)
    Uygulamanın "vitrini" olan ana sayfayı yönetmek için akıllı bir veri çekme stratejisi kurguladık. Burada amaç, kullanıcının önüne "rastgele" değil, "anlamlı" veriler çıkarmaktır.

Trending Algorithm (Trend Mantığı): Bir filmi ne popüler yapar? Bizim sistemimizde popülerlik, dış dünyadaki oylara değil, kulüp içi etkileşime bağlıdır. findTop10ByOrderByClubVoteCountDesc() metodunu kullanarak, en çok puanlanan (yani en çok konuşulan) filmleri üste taşıyoruz.

Eğitici Not: Eğer bir film 10 puan alsa ama sadece 1 kişi oy verse, o film "en iyi" olabilir ama "en popüler" olamaz. Trend listesi, "kalite"den ziyade "hareketlilik" ölçer.

Top-Rated Logic (Kalite Mantığı): Burada devreye clubRating girer. Üyelerimizin verdiği puanların ortalamasını baz alırız. Bu, kulübün "en seçkin" eserlerini listelememizi sağlar.

Performance Optimization (Top 10 & Indexing): Binlerce film arasından sürekli sıralama yapmak veritabanını yorar. Bu yüzden:

Sorguları her zaman Top 10 ile kısıtladık (Limit kullanımı).

OrderBy kullandığımız alanlarda (Rating ve VoteCount) veritabanı indeksleri kullanarak arama hızını milisaniyelere indirdik.

17. Robust Localization (Dayanıklı Çok Dillilik) ve Fallback Stratejisi
    Çok dilli sistemlerde en büyük risk, istenen dilde verinin olmaması durumunda ekranın boş kalmasıdır. Biz bu riski "Fallback" (B Planı) algoritmasıyla yönettik.

İş Akışı:

Sistem önce kullanıcının istediği dili (Accept-Language) arar.

Eğer o dilde bir çeviri yoksa, sistem çökmez veya boş dönmez.

Kodumuz, veritabanında bu film için mevcut olan herhangi bir (ilk bulunan) çeviriyi getirir.

Neden Önemli? Bir kullanıcı Almanca arayüz kullanıyor olabilir ama sisteme yeni eklenen bir Hint filminin henüz Almanca özeti girilmemiş olabilir. Bu durumda kullanıcıya özeti hiç göstermemek yerine, İngilizce veya orijinal özetini sunarak kullanıcıyı bilgiye ulaştırıyoruz.

18. N+1 Sorgu Probleminin SQL Seviyesinde Çözümü
    Puanlama listelerini çekerken performansın darboğaza girmesini önlemek için @EntityGraph kullandık.

Problem: Normalde 20 tane puanı çekerken, her puanın hangi filme ait olduğunu öğrenmek için veritabanına 20 ayrı sorgu atılır (20+1 = 21 sorgu).

Çözüm (Eager Join): @EntityGraph(attributePaths = {"movie"}) kullanarak veritabanına tek bir emir veriyoruz: "Bana puanları getirirken, yanında filmlerini de getir!" * Sonuç: Veritabanı tek bir JOIN işlemiyle tüm bilgiyi paketleyip tek seferde gönderir. Sorgu sayısı 21'den 1'e iner.


19. Modern Frontend Entegrasyonu ve Full-Stack İletişim Standartları
    Bu aşamada uygulama, sadece veri üreten bir yapıdan, son kullanıcıya hizmet veren tam kapsamlı bir platforma (Full-Stack) dönüşmüştür.

Cross-Origin Resource Sharing (CORS) Stratejisi: Güvenlik protokolleri gereği, localhost:3000 (Frontend) ve localhost:8080 (Backend) arasındaki iletişim engellenmiştir. Bu engeli aşmak için Backend tarafında WebConfig üzerinden sadece güvenilir kaynaklara izin veren bir "Beyaz Liste" (Whitelist) mekanizması kurulmuştur.

Axios Interceptor ve Global Authorization: Frontend tarafında her API isteği için manuel token eklemek yerine, merkezi bir Axios Interceptor yapısı kurgulanmıştır. Bu yapı, tarayıcının localStorage alanında geçerli bir JWT varsa, bunu otomatik olarak isteğin Authorization header'ına ekler.

Next.js App Router ve @/ Alias Kullanımı: Proje yapısında "Clean Directory" prensibi benimsenmiş, tüm kaynak kodlar src altına toplanmıştır. Modüller arası bağımlılıklarda karmaşık yollar (../../) yerine, projenin kök dizinini hedefleyen @/ takısı (alias) kullanılarak kodun okunabilirliği artırılmıştır.

TypeScript Sözleşmesi (Type Safety): Java tarafındaki DTO (Data Transfer Object) yapıları, Frontend tarafında interface olarak yeniden tanımlanmıştır. Bu sayede iki dünya arasında "tip güvenliği" sağlanmış, henüz kod yazım aşamasında (Compile-time) veri uyuşmazlığı hatalarının önüne geçilmiştir.

20. Veri Görselleştirme ve Resim Güvenlik Politikası
    Next.js, modern web standartları gereği dış kaynaklardan gelen resimleri (TMDB gibi) doğrudan kabul etmez.

Remote Patterns: next.config.ts üzerinde TMDB sunucuları (image.tmdb.org) güvenilir kaynak olarak tanımlanmıştır. Bu sayede, film afişlerinin yüksek performanslı ve optimize edilmiş şekilde (Next.js Image Optimization) yüklenmesi sağlanmıştır.

Responsive UI: Frontend arayüzü, Tailwind CSS kullanılarak "Mobile First" prensibiyle tasarlanmıştır. Grid sistemi sayesinde filmler; mobilde tek sütun, tablette iki, masaüstünde ise dört sütun şeklinde esnek bir yapıda sunulur.




21. SOCIAL MOVIE CLUB: MIMARI VE API KLAVUZU

    1. Backend Klasör ve Dosya Şeması
       Projemiz Katmanlı Mimari (Layered Architecture) prensibiyle inşa edilmiştir. Her klasörün (package) tek bir sorumluluğu vardır:

       social-movie-app
       ├── src/main/java/com/socialmovieclub
       │   ├── config                  # Uygulama Ayarları
       │   │   ├── LocaleConfig        # Dil (i18n) ayarları
       │   │   ├── MessageConfig       # Mesaj dosyası bağlantıları
       │   │   ├── RestTemplateConfig  # Dış API (TMDB) haberleşme aracı
       │   │   ├── SecurityConfig      # Spring Security & Yetkilendirme kapıları
       │   │   └── WebConfig           # CORS (Frontend erişim) ayarları
       │   ├── controller              # API Giriş Noktaları (Gelen istekleri karşılar)
       │   │   ├── AuthController      # Kayıt ve Giriş işlemleri
       │   │   ├── CommentController   # Yorum CRUD işlemleri
       │   │   ├── GenreController     # Kategori yönetimi
       │   │   ├── MovieController     # Film listeleme (Trend/Top-Rated)
       │   │   ├── RatingController    # Puanlama işlemleri
       │   │   ├── TmdbController      # TMDB'den veri çekme/import
       │   │   └── UserController      # Profil ve kullanıcı bilgileri
       │   ├── core                    # Ortak Çekirdek Yapılar
       │   │   ├── result              # RestResponse (Ortak cevap zarfı)
       │   │   └── utils               # MessageHelper (Mesaj yönetimi)
       │   ├── dto                     # Veri Transfer Objeleri
       │   │   ├── request             # Frontend'den gelen veriler (Input)
       │   │   ├── response            # Frontend'e giden veriler (Output)
       │   │   └── tmdb                # TMDB API'den gelen ham veriler
       │   ├── entity                  # Veritabanı Tablo Modelleri
       │   ├── enums                   # Sabit Değerler (Role: ADMIN, USER vb.)
       │   ├── exception               # Hata Yönetimi (GlobalExceptionHandler)
       │   ├── mapper                  # Entity <-> DTO Dönüştürücüler (MapStruct)
       │   ├── repository              # Veritabanı Sorgu Katmanı (JPA)
       │   ├── security                # JWT ve Güvenlik Filtreleri
       │   └── service                 # İş Mantığı (Business Logic - Kalp burada atar)
       └── src/main/resources
       ├── messages.properties     # Çok dilli mesaj dosyaları (tr, en, de, pt)
       └── application.properties  # Veritabanı ve sistem ayarları  


    2. API Endpoint ve İşleyiş Rehberi
     Uygulamanın Frontend ile nasıl haberleştiğini gösteren detaylı tablo:


Modül,Metod,Endpoint,Görevi,Body / Parametre
Auth,POST,/api/auth/signup,Yeni kullanıcı kaydeder.,UserRegistrationRequest
Auth,POST,/api/auth/login,Giriş yapar ve JWT döner.,LoginRequest
Movies,GET,/api/movies,Tüm filmleri listeler.,Accept-Language (Header)
Movies,GET,/api/movies/trending,En çok oylanan 10 filmi getirir.,Accept-Language (Header)
Movies,GET,/api/movies/top-rated,En yüksek puanlı 10 filmi getirir.,Accept-Language (Header)
Ratings,POST,/api/ratings,Puan verir veya günceller (Upsert).,"RatingRequest (movieId, score)"
Ratings,GET,/api/ratings/me,Kullanıcının kendi puanlarını listeler.,Accept-Language (Header)
Comments,POST,/api/comments,Filmi yorumlar veya cevap yazar.,CommentRequest (parentId ile)
Comments,GET,/api/comments/movie/{id},Bir filme ait yorum ağacını çeker.,movieId (Path)
TMDB,POST,/api/tmdb/import/{id},TMDB'den filmi sisteme kopyalar.,tmdbId (Path)
TMDB,GET,/api/tmdb/search,TMDB'de film aratır.,query (Request Param)


    3. Katmanlar Arası Haberleşme Mantığı (Örnek Akış)
Bir kullanıcı bir filme puan verdiğinde sistemde şu çarklar döner:

Controller: RatingController isteği karşılar, @Valid ile veriyi kontrol eder ve RatingService'e gönderir.

Security: AuthTokenFilter gelen isteğin içindeki JWT'yi çözer, kullanıcının kim olduğunu SecurityContext'e yazar.

Service: RatingService kullanıcının kimliğini token'dan alır. Veritabanında mükerrer kayıt kontrolü yapar (Upsert logic). Puanı kaydeder.

Matematiksel Tetikleme: Puan kaydedildiği an updateMovieRatingStats metodu çalışır; filmin genel ortalaması (clubRating) anında yeniden hesaplanıp güncellenir.

Mapper: Veritabanındaki Rating nesnesi RatingMapper aracılığıyla şık bir RatingResponse DTO'suna dönüştürülür.

Response: RestResponse.success() zarfı içine koyulan veri, kullanıcıya 200 OK koduyla döner.

  4. Hata ve Mesaj Yönetimi
   Merkezi Hata Yakalama: GlobalExceptionHandler tüm BusinessException hatalarını yakalar.

i18n Desteği: Mesajlar doğrudan Java'ya yazılmaz. messages_tr.properties gibi dosyalardan MessageHelper aracılığıyla çekilir.

Örnek Hata: Kullanıcı olmayan bir filme puan vermeye kalkarsa movie.not.found anahtarı ile "Film bulunamadı" mesajı döner.



23. Puan Silme ve Atomik İstatistik Senkronizasyonu (Rating Deletion)
    Sistemde veri silmek, sadece bir satırı yok etmek değil, o veriye bağlı tüm istatistiksel mirası temizlemektir.

Atomic Rollback & Recalculation: Bir puan silindiğinde, updateMovieRatingStats metodu tekrar tetiklenir. Eğer sistemde o filme ait son puan siliniyorsa, SQL AVG fonksiyonu null dönecektir. Kodumuzdaki (avg != null ? avg : 0.0) mantığı sayesinde veritabanı kirlenmez ve film puanı temiz bir şekilde 0.0'a çekilir.

Security Ownership: Silme işlemi sadece movieId alır. userId asla dışarıdan kabul edilmez; sistem token üzerindeki kullanıcı ile veritabanındaki puanın sahibini eşleştirir. Bu Data Ownership prensibidir.

24. Frontend-Backend Haberleşme Sözleşmesi (The API Contract)
    Frontend tarafında karşılaştığımız "Property not found" hatalarını çözmek için kullandığımız mimari:

DTO Synchronization: Backend'de RatingResponse sınıfına eklenen her alan (movieTitle, posterUrl), Frontend tarafındaki types.ts içerisindeki interface ile birebir eşlenmiştir. Bu, projenin Type-Safety (Tip Güvenliği) garantisidir.

Event Bubbling Management: Frontend'de bir kartın (Link) içinde silme butonu (Button) bulunması durumunda e.stopPropagation() ve e.preventDefault() metodları kullanılmıştır. Bu, UI Event Architecture açısından kritik bir dokunuştur; kullanıcının hem detay sayfasına gitme hem de silme işlemini çakışmadan yapabilmesini sağlar.

25. Mapper Katmanındaki Mantıksal Zeka (Mapping Logic)
    RatingMapper ve MovieMapper arasındaki ilişki, Don't Repeat Yourself (DRY) prensibiyle yönetilir:

Poster URL Handler: Veritabanında sadece /v9D...jpg şeklinde saklanan ham veriler, Mapper katmanında @Named("posterUrlMapper") ile işlenir.

Logic: (path.startsWith("/") ? "" : "/"). Bu küçük matematiksel kontrol, TMDB'den gelen verinin formatı ne olursa olsun (başında eğik çizgi olsun veya olmasın) her zaman geçerli bir URL üretilmesini sağlar.

📊 Güncellenmiş Mimari Şema ve İş Akışı
🛰️ Kritik Endpoint Rehberi (Yeni Eklenenler)
Modül	Metod	Endpoint	Görevi	Güvenlik
Ratings	DELETE	/api/ratings/{movieId}	Kullanıcının puanını siler ve ortalamayı günceller.	Authenticated
Ratings	GET	/api/ratings/me	Giriş yapmış kullanıcının tüm geçmişini (Film adı ve posteriyle) döner.	Authenticated

🧠 Önemli Matematiksel ve Mühendislik Mantıkları
Denormalizasyon Kararı: Movie tablosunda clubRating alanını tutmak bir "Redundancy" (Veri tekrarı) gibi görünse de, binlerce istek anında SUM/COUNT işlemi yapmamak için bilinçli bir Performance Trade-off tercihidir.

Upsert Logic (Save or Update):

existingRating=find(userId,movieId)
if(present)→update(score)
else→create(new)
Bu döngü, veritabanında gereksiz satır birikmesini önler.

Hiyerarşik Yorum Ağacı: Yorumlar çekilirken kullanılan Recursive Mapping, karmaşıklığı O(n
2
)'den O(n)'e düşürmek için veritabanı seviyesinde @EntityGraph ile desteklenmiştir.

🛠️ Teknik Borç (Technical Debt) ve Gelecek Planları
Redis Caching: Trend olan 10 film gibi çok sık sorulan ama az değişen veriler için Redis katmanı eklenecek.

Soft Delete for Movies: Filmler silindiğinde onlara bağlı yorum ve puanların havada kalmaması için "Soft Delete" mekanizması Movie tablosuna da taşınacak.

📝 Geliştirici Notu
Bu proje, sadece bir "Film Sitesi" değil; Localization, Security, Relational Mapping ve Performance Optimization konularının harmanlandığı kurumsal bir iskelettir. Her kod satırı, bir sonraki özelliğin (feature) eklenmesini kolaylaştıracak şekilde Scalable (Ölçeklenebilir) olarak yazılmıştır.

🧠 Matematiksel Mantık: Triggered AggregationHer film listelendiğinde 
(Örn: Anasayfa 20 film) veritabanına gidip binlerce satır üzerinden ortalama 
hesaplatmak (AVG) sistemi darboğaza sokar. 
Bu yüzden "Yazma Anında Hesapla, 
Okuma Anında Hazır Sun" prensibini 
uyguladık:
Upsert Mekanizması: findByUserIdAndMovieId kontrolü ile kullanıcı 
daha önce puan vermişse kayıt güncellenir, vermemişse yeni kayıt 
açılır.Hesaplama Tetikleyicisi: Puan kaydedildiği veya silindiği anda 
updateMovieRatingStats(Movie movie) metodu çalışır.SQL Aggregation: Veritabanı seviyesinde SELECT AVG(score), COUNT(id) FROM ratings WHERE movie_id = ... sorgusu tek seferde çalışır.Denormalizasyon: Çıkan sonuçlar doğrudan Movie tablosundaki club_rating ve club_vote_count kolonlarına kalıcı olarak yazılır.Sonuç: Okuma işlemi (GET) matematiksel işlem içermez; sadece hazır kolonu çeker ($O(1)$ karmaşıklığı).


Localization & Fallback Stratejisi (Çok Dillilik)Dinamik film verileri (Başlık, Özet) ve statik mesajlar için hibrit bir i18n yapısı kurulmuştur.Header Tabanlı Seçim: Backend, Accept-Language header'ını (tr, en, pt) yakalar.Mapping Fallback: MovieMapper içinde kurgulanan mantık şudur:Talep edilen dili ara.Yoksa varsayılan dili (en) getir.O da yoksa veritabanındaki mevcut ilk çeviriyi getir.Statik Mesajlar: messages.properties dosyaları ile BusinessException mesajları merkezi olarak yönetilir.4. Gelişmiş Mapping ve GüvenlikMapping Isolation: qualifiedByName kullanarak posterUrl ve imdbUrl alanlarının birbirine bulaşması engellendi. TMDB'den gelen ham pathler (/v9D...), mapper seviyesinde tam URL'e dönüştürülür.Security Context Integration: Puanlama ve silme işlemlerinde userId asla dışarıdan alınmaz; JWT üzerinden sunucu tarafında çözülür. Bu, ID Spoofing saldırılarını mimari olarak engeller.Rating Deletion (Puan Kaldırma): Kullanıcı puanını sildiğinde sistem sadece veriyi silmez; istatistik motorunu tekrar tetikleyerek filmin genel puanını ve oy sayısını aşağı yönlü revize eder.5. API Klavuzu ve Endpoint Şeması📂 Proje Klasör YapısıPlaintextsocial-movie-app
├── config/              # Security, CORS, i18n, RestTemplate Ayarları
├── controller/          # API Giriş Noktaları (Auth, Movie, Rating, Comment, TMDB)
├── core/                # RestResponse (Cevap Zarfı) ve MessageHelper
├── dto/                 # Request (Input), Response (Output), Tmdb (Ham Veri)
├── entity/              # Movie, MovieTranslation, Rating, Comment, User, Genre
├── mapper/              # MapStruct Arayüzleri (MovieMapper, RatingMapper vb.)
├── repository/          # JPA Query Katmanı (EntityGraph ile N+1 çözümü)
└── service/             # Business Logic & Matematiksel Hesaplamalar
📋 Kritik EndpointlerModülMetodEndpointGöreviİş MantığıAuthPOST/api/auth/signupKayıtBCrypt ile şifre hashlemeAuthPOST/api/auth/loginGirişJWT Token üretimiMoviesGET/api/movies/trendingTrendlerEn çok oylanan 10 filmMoviesGET/api/movies/top-ratedEn İyilerEn yüksek puanlı 10 filmRatingsPOST/api/ratingsPuanlaUpsert + Stat GüncellemeRatingsDELETE/api/ratings/{movieId}Puan SilDelete + Stat GüncellemeRatingsGET/api/ratings/meProfilimPuanladığım filmleri listeleCommentsPOST/api/commentsYorumlaSelf-referencing Reply yapısıTMDBPOST/api/tmdb/import/{id}İçe AktarÇok dilli kayıt (Atomic Transaction)6. N+1 Sorgu Problemi ve ÇözümüProfil sayfasında kullanıcının tüm puanlarını çekerken, her puan için veritabanına ayrı ayrı film sorgusu atmamak (20+1 problem) için @EntityGraph kullanılmıştır:Normal Sorgu: $N+1$ sorgu.Optimized Sorgu: @EntityGraph(attributePaths = {"movie"}) ile tek bir JOIN sorgusu. Bu, veritabanı performansını %90 oranında artırır.7. Mevcut Durum ÖzetiUygulama şu an;✅ Güvenli: JWT ve sunucu tarafı kimlik doğrulama ile korunuyor.✅ Tutarlı: Puan silme/ekleme anında tüm istatistikler senkronize ediliyor.✅ Esnek: 4 farklı dilde veri sunabiliyor ve hataları yönetebiliyor.✅ Performanslı: Denormalizasyon ve Eager Fetching stratejileri ile binlerce isteği karşılamaya hazır.


Mevcut Durum Notu:
Backend tarafındaki tüm bu "makine dairesi" ayarları, 
Frontend'deki Next.js uygulamamızın hatasız çalışması için gereken veriyi (Data) ve 
güvenliği (Security) sağlar. Frontend tarafında movieService.getTrendingMovies() 
çağırdığımızda aslında yukarıdaki şemada bulunan MovieController -> /trending hattını tetiklemiş oluyoruz.



Sıradaki Adım: Frontend'de src/app/login/page.tsx ve src/app/register/page.tsx sayfalarını oluşturup 
Backend'deki /api/auth endpoint'lerini bağlayacağız.


22. Çok Dilli Veri Yönetimi (Multi-language Data Management)
    Proje, hem statik mesajlar hem de dinamik film verileri için tam kapsamlı bir i18n (Internationalization) yapısına sahiptir.

1. Dinamik Çeviri Yaklaşımı (Dynamic Translation)
   Filmler sisteme kaydedilirken (Import), sadece istek anındaki dil ile değil, sistemin desteklediği tüm dillerle birlikte kaydedilir.

Desteklenen Diller: TmdbService içindeki supportedLanguages listesi ile merkezi olarak yönetilir.

Atomik Kayıt: Bir film için TMDB API'sine desteklenen her dil için (tr, en, de vb.) ayrı çağrılar yapılır ve tüm MovieTranslation kayıtları tek bir transaction içinde veritabanına işlenir.

2. Header Tabanlı Dil Seçimi
   Backend, istemciden (Frontend/Postman) gelen Accept-Language header'ını okur.

Interceptor Seviyesi: Spring LocaleContextHolder aracılığıyla gelen dil kodu yakalanır.

Mapper Seviyesi: MovieMapper nesnesi, veritabanındaki çeviri tablosundan o dile ait olan başlık ve açıklamayı otomatik olarak seçer ve DTO'ya map eder. Eğer talep edilen dilde çeviri yoksa (Fallback), sistem varsayılan olarak en verisini döner.





Mevcut Durum Değerlendirmesi

Auth Akışı: Kayıt ol -> Giriş yap -> Token al.

Veri Akışı: TMDB'den film çek -> Veritabanına kaydet (Kategoriler ve Çevirilerle birlikte).

Etkileşim Akışı: Filme puan ver -> Ratings tablosunu güncelle -> Movies tablosundaki istatistikleri otomatik tetikle (Trigger).

Sunum Akışı: Trend olanları getir -> En iyi puan alanları getir -> Benim puanlarımı listele.

Bu akışların her biri birer API Endpoint olarak Postman'de test edildi ve onaylandı.

Şu an Backend (Arka Plan) mimarimiz, bir sosyal medya platformunun taşıması gereken tüm yükleri 
kaldırabilecek kapasitede: 
✅ Güvenli (JWT & Security Context) 
✅ Hızlı (Aggregation & Eager Fetching) 
✅ Esnek (Multi-language & Fallback) 
✅ Tutarlı (Transactional & Upsert Logic)

"Frontend mimarisi büyüdükçe SOLID prensipleri gereği Hook ve Atomic Component yapısına geçilmiştir. Detaylar Frontend Guide içindedir."
