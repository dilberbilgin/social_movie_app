🚀 Social Movie Club: Teknik Mimari ve Gelişim Raporu
1. Proje Yapısı ve Katmanlar (Folder Structure)
   Proje, her katmanın sorumluluğunun net ayrıldığı (Decoupled) bir yapıda inşa edilmiştir:

config: Uygulamanın çalışma kuralları burada belirlenir. SecurityConfig (Erişim kontrolü), RestTemplateConfig (Dış API istemcisi), LocaleConfig (Çoklu dil desteği) ve @EnableAsync (Asenkron işlem altyapısı) yapılandırmaları tamamlanmıştır.

controller: API'nin giriş kapısıdır. TmdbController dış kaynaklı, MovieController ise iç kaynaklı film operasyonlarını yönetir.

dto: Veri transfer nesneleridir. tmdb paketi dış API'den gelen ham veriyi, request dışarıdan gelen emirleri, response ise son kullanıcıya giden temizlenmiş veriyi temsil eder.

entity: Veritabanı modelidir. BaseEntity (ID, tarih takibi) ve BaseTranslation (Dil bağımsız çeviri altyapısı) kullanılarak kalıtım yoluyla kod tekrarı önlenmiştir.

exception: Hata yönetimidir. BusinessException ile iş mantığı hataları, GlobalExceptionHandler ile tüm sistem hataları standart bir JSON formatına (RestResponse) dönüştürülür.

mapper: MapStruct katmanıdır. Kompleks dönüşüm mantıkları (dil seçimi, URL birleştirme) burada merkezi olarak yönetilir.

repository: Veritabanı erişimidir. Spring Data JPA gücüyle, karmaşık sorgular (@EntityGraph gibi) optimize edilerek yazılmıştır.

service: Uygulamanın kalbidir. İş mantığı, dış API entegrasyonu ve veritabanı koordinasyonu burada gerçekleşir.

2. Geliştirilen Kritik Fonksiyonlar ve Metodlar
   A. TMDB Katmanı (TmdbService)
   syncGenres(lang): TMDB'deki türleri (Genre) sistemimize aktarır. existsByTmdbId kontrolü ile mükerrer kayıt engellenir, Optional<Genre> ile veritabanı güvenliği sağlanır.

searchMovies(query, lang): UriComponentsBuilder kullanarak güvenli (Encoded) URL'ler oluşturur ve TMDB'den anlık film listesi getirir.

importMovie(tmdbId, lang): Bir filmi "Sıfırdan Sisteme Kazandırma" metodudur. Detayları çeker, türleri eşleştirir, çeviri kaydı oluşturur ve atomik bir işlem olarak kaydeder.

B. Mapper Katmanı (MovieMapper)
handlePosterUrl: https://image.tmdb.org/t/p/w500 sabitini path ile güvenli birleştirir. Path boşsa https://via.placeholder.com üzerinden "No Poster" görseli döner.

setLanguageFields: Akıllı Fallback algoritmasıdır:

İstenen dili (Accept-Language) ara.

Yoksa "en" (İngilizce) ara.

O da yoksa mevcut ilk çeviriyi getir.

extractYear: String tarih bilgisinden (1999-05-19) güvenli bir şekilde ilk 4 karakteri (Integer) ayıklar, hata durumunda uygulamayı çökertmez.

3. Dikkat Edilen Kritik Detaylar
   N+1 Sorgu Problemi Çözümü: MovieRepository içinde @EntityGraph(attributePaths = {"genres", "translations"}) kullanılarak, film listesi çekilirken her film için tekrar tekrar veritabanına gidilmesi engellenmiş, tek sorgu ile (JOIN) performans optimize edilmiştir.

Veri Tutarlılığı (Integrity): tmdb_id alanı üzerinden benzersizlik (Unique) kontrolü yapılarak veri kirliliği önlenmiştir.

Tür Uyuşmazlığı Yönetimi: TMDB'den gelen Long ID'ler ile bizim sistemdeki UUID ID'ler arasındaki köprü, Mapper katmanında ignore = true ve manuel eşleme teknikleriyle kurulmuştur.

Localization (Çok Dillilik): MessageHelper ile sadece veriler değil, sistemin verdiği tüm cevaplar da kullanıcı diline duyarlı hale getirilmiştir.

URL Güvenliği: Dinamik URL oluşturma süreçlerinde String.format yerine UriComponentsBuilder tercih edilerek boşluk ve özel karakter hataları (Bad Request) elenmiştir.

Set vs List: Entity sınıflarında Set kullanılarak, Hibernate tarafındaki MultipleBagFetchException hatası baştan engellenmiş ve veri tekrarının önüne geçilmiştir.

4. Mevcut Aşama ve Yapılacaklar
   Şu anki Durum: Sistem, dış dünyadan (TMDB) akıllı bir şekilde veri çekebilen, bu veriyi çok dilli saklayabilen ve performanslı bir şekilde listeleyebilen profesyonel bir kütüphane altyapısına sahiptir.


5. Kullanıcı ve Güvenlik Altyapısı (User & Security Layer)
   Uygulamanın "Sosyal" bir platforma dönüşebilmesi için gerekli olan kimlik yönetimi ve güvenlik duvarı Stateless (Durumsuz) bir mimari ile kurulmuştur:

JWT (JSON Web Token) Entegrasyonu: Kullanıcı oturumları sunucu tarafında saklanmaz (Stateless). JwtUtils sınıfı ile token üretimi ve doğrulaması yapılır. AuthTokenFilter her istekte token'ı kontrol ederek kullanıcıyı sisteme tanıtır.

Akıllı Yetkilendirme (SecurityConfig): * Filmleri ve yorumları görüntüleme herkese açık (Public).

Yorum yapma ve silme gibi aksiyonlar sadece doğrulanmış (Authenticated) kullanıcılara özeldir.

Hata Yakalama (AuthEntryPointJwt): Yetkisiz erişimlerde sadece "401 Unauthorized" dönmek yerine, MessageHelper aracılığıyla kullanıcının neden reddedildiğini (Örn: 1016: Session expired) bildiren, dil desteğine sahip ve kurumsal kodlu hata mesajları dönülmektedir.

6. Sosyal Etkileşim: Yorum Sistemi (Comment System)
   Instagram, Facebook ve Discord gibi modern platformların kullandığı Self-Referencing (Özyinelemeli) yapı kullanılarak hiyerarşik bir yorum sistemi inşa edilmiştir:

Hiyerarşik Yapı: Comment entity'si kendi kendisine referans vererek (parent_id) ana yorum ve alt yorum (cevap) ilişkisini tutar.

Recursive Mapping: MapStruct üzerindeki CommentMapper, bir yorumu dönerken içindeki tüm alt yorumları (replies) otomatik olarak derinlemesine döner. Bu sayede sonsuz derinlikte cevaplaşma desteği sağlanır.

Soft Delete: Veritabanından fiziksel silme yapmak yerine deleted flag'i kullanılır. Silinen yorumun içeriği comment.content.deleted anahtarı ile "Bu yorum kullanıcı tarafından silindi" şeklinde gizlenir, böylece altındaki cevap hiyerarşisi bozulmaz.

Veri Tutarlılığı ve Senkronizasyon: Kayıt anında veritabanındaki otomatik oluşan alanların (ID, CreatedDate) anlık olarak DTO'ya yansıması için saveAndFlush mekanizması ve @Transactional yönetimi optimize edilmiştir.

7. Global Mesaj ve Hata Kodlama Standardı
   Projenin tamamında 1000'li hata kodları standardı getirilmiştir. Bu sayede Frontend ekibi, metinlerden bağımsız olarak hata kodlarına göre (Örn: 1014, 1017) özel UI aksiyonları (Toast, Modal, Redirect) geliştirebilir:

1000-1010: Genel ve Movie hataları.

1011-1016: Auth ve Güvenlik hataları.

1017-1021: Yorum (Comment) ve Kullanıcı hataları.

8. Mevcut Aşama ve Yapılacaklar (Güncel)
   Şu anki Durum: Sistem artık sadece bir kütüphane değil, kullanıcıların birbirleriyle etkileşime girebildiği, güvenli ve hiyerarşik veri yapısına sahip yaşayan bir platformdur.

9. Veri Temizliği ve URL Standardizasyonu (Data Sanitization)
   Sistemin dış API'lerden (TMDB) aldığı verilerin güvenilirliğini ve görselliğini artırmak amacıyla "Mapping Isolation" mimarisi uygulanmıştır:

Named Mapping & QualifiedByName: MapStruct katmanında farklı alanların (Title, Poster, IMDb ID) aynı veri tipine (String) sahip olması nedeniyle oluşan "yanlış eşleşme" (mismatch) sorunu @Named anotasyonu ile çözülmüştür. Bu sayede poster URL'i oluşturan metodun, yanlışlıkla film başlığının başına URL eklemesi (Contamination) %100 engellenmiştir.

Poster URL Standardizasyonu: TMDB'den gelen / ile başlayan relative path'ler, posterUrlMapper ile dinamik olarak https://image.tmdb.org/t/p/w500 prefix'i ile birleştirilir. Görseli olmayan filmler için sistem otomatik olarak profesyonel bir "No Poster" placeholder'ı üretir.

İzole Çeviri Yönetimi: originalTitle (filmin dünya genelindeki adı) ile title (kullanıcının seçtiği dildeki adı) alanları birbirinden ayrılmıştır. Bu, veritabanında arama yaparken ve listeleme yaparken tutarlılığı sağlar.

10. IMDb Entegrasyonu ve Harici Bağlantı Yönetimi
    Letterboxd tarzı "tıklanabilir dış dünya bağlantıları" için özel bir servis katmanı kurgulanmıştır:

IMDb ID Extraction: TMDB'nin detay API'sinden gelen imdb_id (Örn: tt1375666) veritabanına ham haliyle kaydedilir. Bu, ileride farklı IMDb servisleriyle entegrasyonu kolaylaştırır.

Dynamic URL Generator: Kullanıcıya dönen Response DTO içinde imdbUrl alanı, veritabanında tutulmak yerine çalışma anında (On-the-fly) imdbUrlGenerator metodu ile üretilir. Bu sayede domain değişikliği veya URL yapısı güncellendiğinde tüm veritabanını update etmek yerine sadece Mapper üzerindeki tek satır kodu değiştirmek yeterli hale getirilmiştir.

Arama vs. İçe Aktarma Farklılaştırması: Performans optimizasyonu için "Arama" (Search) anında gelmeyen IMDb bilgileri, "İçe Aktarma" (Import) anında atomik bir şekilde çekilerek veritabanına işlenir.


Sıradaki Hedefler:

Rating & Metrics (Kritik): Kullanıcıların 1-10 (veya yarım yıldızlı 0.5-5.0) puan verebildiği Rating sistemi.

Calculated Stats: Her film için clubRating (ortalama puan) ve clubVoteCount alanlarının, her yeni puan verildiğinde asenkron veya tetiklemeli olarak güncellenmesi.

Frontend Integration: Geliştirilen bu sağlam API'nin bir arayüz (React/Next.js) ile buluşturulması.