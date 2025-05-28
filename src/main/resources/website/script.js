document.addEventListener('DOMContentLoaded', function() {
    const form = document.getElementById('verification-form');
    if (form) {
        form.addEventListener('submit', function(event) {
            const recaptchaResponse = grecaptcha.getResponse();
            if (!recaptchaResponse) {
                event.preventDefault();
                alert('Please complete the CAPTCHA verification');
            }
        });
    }

    createParticles();
});

function createParticles() {
    const particlesContainer = document.querySelector('.particles');
    if (!particlesContainer) return;

    const particleCount = 15;
    for (let i = 0; i < particleCount; i++) {
        const particle = document.createElement('div');
        particle.classList.add('particle');

        // Random styling
        particle.style.position = 'absolute';
        particle.style.width = Math.random() * 5 + 2 + 'px';
        particle.style.height = particle.style.width;
        particle.style.backgroundColor = 'rgba(255, 255, 255, ' + (Math.random() * 0.3 + 0.1) + ')';
        particle.style.borderRadius = '50%';

        // Random position
        particle.style.left = Math.random() * 100 + 'vw';
        particle.style.top = Math.random() * 100 + 'vh';

        // Animation
        particle.style.animation = 'floatParticle ' + (Math.random() * 15 + 10) + 's linear infinite';
        particle.style.animationDelay = Math.random() * 5 + 's';

        particlesContainer.appendChild(particle);
    }
}

// Add floating animation for particles
const styleSheet = document.createElement('style');
styleSheet.textContent = `
    @keyframes floatParticle {
        0% {
            transform: translate(0, 0);
            opacity: 0;
        }
        10% {
            opacity: 1;
        }
        90% {
            opacity: 1;
        }
        100% {
            transform: translate(${Math.random() * 200 - 100}px, -100vh);
            opacity: 0;
        }
    }
    
    .particle {
        position: absolute;
        pointer-events: none;
    }
`;
document.head.appendChild(styleSheet);