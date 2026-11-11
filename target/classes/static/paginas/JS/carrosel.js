const carouselImages = [
      {
        src: "https://images.unsplash.com/photo-1522202176988-66273c2fd55f?q=80&w=1200&auto=format&fit=crop",
        alt: "Estudantes estudando juntos"
      },
      {
        src: "https://images.unsplash.com/photo-1523240795612-9a054b0db644?q=80&w=1200&auto=format&fit=crop",
        alt: "Grupo de estudantes"
      },
      {
        src: "https://images.unsplash.com/photo-1516321318423-f06f85e504b3?q=80&w=1200&auto=format&fit=crop",
        alt: "Pessoa estudando no computador"
      },
      {
        src: "https://images.unsplash.com/photo-1434030216411-0b793f4b4173?q=80&w=1200&auto=format&fit=crop",
        alt: "Pessoa estudando e fazendo anotações"
      },
      {
        src: "https://images.unsplash.com/photo-1531482615713-2afd69097998?q=80&w=1200&auto=format&fit=crop",
        alt: "Equipe trabalhando e estudando"
      }
    ];

    let currentImage = 0;
    let autoPlay;

    const carouselImage = document.getElementById("carousel-image");
    const previousButton = document.getElementById("carousel-prev");
    const nextButton = document.getElementById("carousel-next");
    const indicatorsContainer = document.getElementById("carousel-indicators");

    function createIndicators() {
      carouselImages.forEach((image, index) => {
        const indicator = document.createElement("button");

        indicator.type = "button";
        indicator.className = "carousel-indicator";
        indicator.setAttribute("aria-label", `Ir para imagem ${index + 1}`);

        indicator.addEventListener("click", () => {
          showImage(index);
          restartAutoPlay();
        });

        indicatorsContainer.appendChild(indicator);
      });
    }

    function showImage(index) {
      currentImage = (index + carouselImages.length) % carouselImages.length;

      carouselImage.classList.add("changing");

      setTimeout(() => {
        carouselImage.src = carouselImages[currentImage].src;
        carouselImage.alt = carouselImages[currentImage].alt;
        carouselImage.classList.remove("changing");
      }, 180);

      document.querySelectorAll(".carousel-indicator").forEach((indicator, i) => {
        indicator.classList.toggle("active", i === currentImage);
      });
    }

    function nextImage() {
      showImage(currentImage + 1);
    }

    function previousImage() {
      showImage(currentImage - 1);
    }

    function restartAutoPlay() {
      clearInterval(autoPlay);
      autoPlay = setInterval(nextImage, 5000);
    }

    nextButton.addEventListener("click", () => {
      nextImage();
      restartAutoPlay();
    });

    previousButton.addEventListener("click", () => {
      previousImage();
      restartAutoPlay();
    });

    document.querySelector(".hero-carousel").addEventListener("mouseenter", () => {
      clearInterval(autoPlay);
    });

    document.querySelector(".hero-carousel").addEventListener("mouseleave", () => {
      restartAutoPlay();
    });

    document.addEventListener("keydown", (event) => {
      if (event.key === "ArrowRight") {
        nextImage();
        restartAutoPlay();
      }

      if (event.key === "ArrowLeft") {
        previousImage();
        restartAutoPlay();
      }
    });

    document.getElementById("btn-saiba-mais").addEventListener("click", () => {
      document.querySelector(".info-grid").scrollIntoView({
        behavior: "smooth",
        block: "center"
      });
    });

    createIndicators();
    showImage(0);
    restartAutoPlay();