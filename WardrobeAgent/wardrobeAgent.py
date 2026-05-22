import streamlit as st
import os
import time
import requests
from langchain_openai import ChatOpenAI
from langchain_community.agent_toolkits.openapi.planner import create_openapi_agent
from langchain_community.agent_toolkits.openapi.spec import reduce_openapi_spec
from langchain_community.utilities.requests import RequestsWrapper

# --- 1. Configuration ---
st.set_page_config(page_title="AI Wardrobe Assistant", page_icon="👗")
os.environ["OPENAI_API_KEY"] = "OPENAI_API_KEY_PLACEHOLDER"

# The URL where your Java Spring Boot app serves the Swagger/OpenAPI docs
JAVA_API_DOCS_URL = "http://localhost:8080/v3/api-docs"

# System prompt to define the agent's persona and context
SYSTEM_PROMPT = """
You are a Wardrobe Stylist.
Your goal is to help users pick outfits from their available wardrobe.

The wardrobe is organised into these exact categories (use these as the {category} value in all API calls):
- shirts   (tops, blouses, t-shirts, sweaters, etc.)
- jackets  (outerwear, coats, blazers, etc.)
- pants    (trousers, jeans, shorts, skirts, etc.)
- shoes    (all footwear)
- wardrobe (miscellaneous items that don't fit the above)

If the user asks for an outfit, you must always recommend: a shirt, a pair of pants, and shoes.
In case you cannot find a suitable item in a category, suggest the closest available item but note it is not ideal.
Always consult the wardrobe API to see what items are available before making suggestions.
If the user wants to add an item, use POST /api/wardrobe/{category}/add with the correct category from the list above.
If the user wants to remove an item, use DELETE /api/wardrobe/{category}/{id} with the correct category.
If the user wants to see all items, use GET /api/wardrobe/all.
"""

# --- 2. Initialize the Agent ---
@st.cache_resource
def setup_agent():
    # 1. Fetch the OpenAPI spec from your Java backend
    response = requests.get(JAVA_API_DOCS_URL)
    raw_spec = response.json()
    
    # 2. Reduce the spec to save tokens and keep the LLM focused
    spec = reduce_openapi_spec(raw_spec)
    
    # 3. Initialize LLM and Requests Wrapper
    llm = ChatOpenAI(model="gpt-4o-mini", temperature=0)
    requests_wrapper = RequestsWrapper()
    
    # 4. Create the OpenAPI Agent
    # allow_dangerous_requests=True is required for local API interaction
    agent = create_openapi_agent(
        llm=llm,
        api_spec=spec,
        requests_wrapper=requests_wrapper,
        verbose=True, 
        allow_dangerous_requests=True
    )
    return agent

# --- 3. Streamlit UI ---
st.title("👗 AI Wardrobe Stylist")
st.subheader("Your AI-powered personal closet assistant")

# Initialize agent
try:
    agent_executor = setup_agent()
    st.success("Connected to your Java Wardrobe API!")
except Exception as e:
    st.error(f"Error: {e}\n\nIs your Spring Boot app running at {JAVA_API_DOCS_URL}?")
    st.stop()

# User Interaction
user_input = st.text_input("What are you dressing up for today?", placeholder="e.g., A formal dinner in chilly weather")

if user_input:
    with st.chat_message("user"):
        st.write(user_input)
        
    with st.chat_message("assistant"):
        with st.spinner("Analyzing your closet..."):
            try:
                full_prompt = f"{SYSTEM_PROMPT}\n\nUser Request: {user_input}"
                t0 = time.perf_counter()
                result = agent_executor.invoke(full_prompt)
                elapsed = time.perf_counter() - t0
                print(f"[TIMING] Total answer time: {elapsed:.3f}s")
                st.write(result["output"])
            except Exception as e:
                st.error(f"An error occurred: {str(e)}")

# Sidebar Info
with st.sidebar:
    st.info("This agent uses your Java Spring Boot API to fetch clothing data from CouchDB.")