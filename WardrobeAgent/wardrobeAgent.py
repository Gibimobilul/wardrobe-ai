import streamlit as st
import os
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
If the user asks for an outfit, you must always recommend: a top item (shirt, blouse, sweater, etc.), a bottom item (pants, skirt, shorts, etc.), and a pair of shoes.
In case you cannot find a suitable item of the 3 mandatory categories, you can search and suggest any item in that category, but specify that it is not ideal.
Dresses or similar are top items. If you recommend a dress or similar for the top item, you must skip the bottom item.
Always consult the wardrobe API to see what items are available before making suggestions.
If the user wants to add an item to their wardrobe, you can use the POST /api/wardrobe/add endpoint.
If the user wants to remove an item from their wardrobe, you can use the DELETE /api/wardrobe/delete/{id} endpoint.
If the user wants to see all items, you can use the GET /api/wardrobe/all endpoint.
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
                # The agent logic:
                # 1. Reads the user input.
                # 2. Checks OpenAPI spec for a search tool.
                # 3. Calls your Java GET /api/wardrobe/search.
                # 4. Formulates a recommendation.
                full_prompt = f"{SYSTEM_PROMPT}\n\nUser Request: {user_input}"
                result = agent_executor.invoke(full_prompt)
                st.write(result["output"])
            except Exception as e:
                st.error(f"An error occurred: {str(e)}")

# Sidebar Info
with st.sidebar:
    st.info("This agent uses your Java Spring Boot API to fetch clothing data from Elasticsearch.")